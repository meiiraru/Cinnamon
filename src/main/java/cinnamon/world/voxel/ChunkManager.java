package cinnamon.world.voxel;

import cinnamon.render.Camera;
import cinnamon.render.MatrixStack;
import cinnamon.render.WaterRenderer;
import cinnamon.render.WorldRenderer;
import cinnamon.render.shader.Shader;
import cinnamon.render.shader.Shaders;
import cinnamon.render.texture.Texture;
import cinnamon.utils.AABB;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.*;
import java.util.concurrent.*;

import static org.lwjgl.opengl.GL11.*;

/**
 * Manages chunk lifecycle: loading, unloading, meshing, and rendering.
 * <p>
 * Each tick, determines which chunks should be loaded around the player
 * and which should be unloaded. Limits remeshes per frame to avoid spikes.
 * <p>
 * Rendering iterates loaded chunks, frustum-culls at chunk level, and
 * draws each visible chunk mesh with a single draw call.
 */
public class ChunkManager {

    /** Render distance in chunks (e.g. 8 = 128 blocks) */
    private int renderDistanceChunks = 10;

    /** Maximum number of GL mesh uploads per frame (to avoid GPU stalls) */
    private static final int MAX_UPLOADS_PER_FRAME = 32;

    /** All loaded chunks keyed by packed coordinates */
    private final Map<Long, VoxelChunk> chunks = new HashMap<>();

    /** Thread pool for async generation and meshing */
    private final ExecutorService chunkPool;

    /** Chunks that completed meshing on worker threads, awaiting GL upload on main thread */
    private final Queue<VoxelChunk> uploadQueue = new ConcurrentLinkedQueue<>();

    /** Chunks whose block data is ready but need mesh building (checked on main thread) */
    private final Set<VoxelChunk> needsMesh = ConcurrentHashMap.newKeySet();

    /** Reusable identity matrix for the model transform */
    private final Matrix4f modelMatrix = new Matrix4f();
    private final Matrix3f normalMatrix = new Matrix3f();

    /** Last known player chunk position (for load/unload decisions) */
    private int lastPlayerCX = Integer.MIN_VALUE;
    private int lastPlayerCY = Integer.MIN_VALUE;
    private int lastPlayerCZ = Integer.MIN_VALUE;

    // Height limits in chunks
    private static final int MIN_CHUNK_Y = -2;  // -32 blocks
    private static final int MAX_CHUNK_Y = 8;   // 128 blocks

    public ChunkManager() {
        this(10);
    }

    public ChunkManager(int renderDistanceChunks) {
        this.renderDistanceChunks = renderDistanceChunks;
        int poolSize = Math.max(1, Math.min(4, Runtime.getRuntime().availableProcessors() - 2));
        this.chunkPool = Executors.newFixedThreadPool(poolSize, r -> {
            Thread t = new Thread(r, "ChunkWorker");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Update chunk loading/unloading based on player position.
     * Call once per tick.
     */
    public void update(Vector3f playerPos) {
        int pcx = (int) Math.floor(playerPos.x / VoxelChunk.SIZE);
        int pcy = (int) Math.floor(playerPos.y / VoxelChunk.SIZE);
        int pcz = (int) Math.floor(playerPos.z / VoxelChunk.SIZE);

        // Only re-evaluate if player moved to a new chunk
        if (pcx == lastPlayerCX && pcy == lastPlayerCY && pcz == lastPlayerCZ) {
            processQueues();
            return;
        }

        lastPlayerCX = pcx;
        lastPlayerCY = pcy;
        lastPlayerCZ = pcz;

        // Determine which chunks should be loaded
        Set<Long> desiredChunks = new HashSet<>();
        for (int cx = pcx - renderDistanceChunks; cx <= pcx + renderDistanceChunks; cx++) {
            for (int cz = pcz - renderDistanceChunks; cz <= pcz + renderDistanceChunks; cz++) {
                // Circular distance check in XZ plane
                int dx = cx - pcx;
                int dz = cz - pcz;
                if (dx * dx + dz * dz > renderDistanceChunks * renderDistanceChunks) continue;

                for (int cy = MIN_CHUNK_Y; cy < MAX_CHUNK_Y; cy++) {
                    desiredChunks.add(VoxelChunk.packKey(cx, cy, cz));
                }
            }
        }

        // Unload chunks that are no longer desired
        Iterator<Map.Entry<Long, VoxelChunk>> it = chunks.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Long, VoxelChunk> entry = it.next();
            if (!desiredChunks.contains(entry.getKey())) {
                VoxelChunk chunk = entry.getValue();
                chunk.unlinkNeighbors();
                chunk.free();
                it.remove();
            }
        }

        // Load new chunks that aren't loaded yet
        for (long key : desiredChunks) {
            if (!chunks.containsKey(key)) {
                // Decode key back to coordinates
                int cx = (int) ((key << 43) >> 43); // sign-extend 21 bits
                int cy = (int) ((key << 22) >> 42); // sign-extend 22 bits from position 21
                int cz = (int) ((key << 0) >> 43);  // sign-extend 21 bits from position 43

                // Actually just recalculate from the loops
                // The key decoding is tricky with bit packing, so let's just search
                // We'll use a simpler approach: store coordinates in the chunk itself
                // and use the loop values
            }
        }

        // Simpler approach: iterate the desired area again for new chunks
        for (int cx = pcx - renderDistanceChunks; cx <= pcx + renderDistanceChunks; cx++) {
            for (int cz = pcz - renderDistanceChunks; cz <= pcz + renderDistanceChunks; cz++) {
                int dx = cx - pcx;
                int dz = cz - pcz;
                if (dx * dx + dz * dz > renderDistanceChunks * renderDistanceChunks) continue;

                for (int cy = MIN_CHUNK_Y; cy < MAX_CHUNK_Y; cy++) {
                    long key = VoxelChunk.packKey(cx, cy, cz);
                    if (!chunks.containsKey(key)) {
                        VoxelChunk chunk = new VoxelChunk(cx, cy, cz);
                        chunks.put(key, chunk);
                        submitGeneration(chunk);
                    }
                }
            }
        }

        // Link neighbors for newly loaded chunks
        linkAllNeighbors();

        processQueues();
    }

    /**
     * Process async results: submit mesh tasks for generated chunks and upload
     * completed meshes to the GPU. Only GL work (uploads) happens here on the main thread.
     */
    private void processQueues() {
        // 1. Check generated chunks and submit mesh tasks when all neighbors are ready
        Iterator<VoxelChunk> meshIt = needsMesh.iterator();
        while (meshIt.hasNext()) {
            VoxelChunk chunk = meshIt.next();

            // Skip if chunk was unloaded
            if (chunks.get(chunk.getKey()) != chunk) {
                meshIt.remove();
                continue;
            }

            // Only process chunks in GENERATED state
            if (chunk.state != VoxelChunk.ChunkState.GENERATED) {
                meshIt.remove();
                continue;
            }

            // Wait until all existing neighbors have been generated
            if (!canMesh(chunk)) continue;

            meshIt.remove();
            submitMesh(chunk);
        }

        // 2. Upload completed mesh data to GPU (limited per frame to avoid stalls)
        int uploads = 0;
        while (!uploadQueue.isEmpty() && uploads < MAX_UPLOADS_PER_FRAME) {
            VoxelChunk chunk = uploadQueue.poll();
            if (chunk == null) break;

            // Skip if chunk was unloaded while meshing
            if (chunks.get(chunk.getKey()) != chunk) continue;

            ChunkMesher.MeshBuildResult result = chunk.pendingMeshResult;
            chunk.pendingMeshResult = null;

            if (result != null) {
                // Upload opaque mesh
                if (result.hasOpaque()) {
                    if (chunk.getMesh() != null) {
                        chunk.getMesh().update(result.opaqueVertices(), result.opaqueIndices());
                    } else {
                        chunk.setMesh(new ChunkMesh(result.opaqueVertices(), result.opaqueIndices()));
                    }
                } else {
                    chunk.setMesh(null);
                }

                // Upload water mesh
                if (result.hasWater()) {
                    if (chunk.getWaterMesh() != null) {
                        chunk.getWaterMesh().update(result.waterVertices(), result.waterIndices());
                    } else {
                        chunk.setWaterMesh(new ChunkMesh(result.waterVertices(), result.waterIndices()));
                    }
                } else {
                    chunk.setWaterMesh(null);
                }
            } else {
                chunk.setMesh(null);
                chunk.setWaterMesh(null);
            }

            chunk.state = VoxelChunk.ChunkState.READY;
            chunk.clearDirty();
            uploads++;

            // If the chunk was dirtied while meshing, schedule another remesh
            if (chunk.isDirty()) {
                chunk.state = VoxelChunk.ChunkState.GENERATED;
                needsMesh.add(chunk);
            }
        }
    }

    /**
     * Submit a generation task to the thread pool.
     */
    private void submitGeneration(VoxelChunk chunk) {
        chunk.state = VoxelChunk.ChunkState.GENERATING;
        chunkPool.submit(() -> {
            try {
                VoxelWorldGenerator.generateChunk(chunk);
                chunk.state = VoxelChunk.ChunkState.GENERATED;
                if (!chunk.isEmpty()) {
                    needsMesh.add(chunk);
                } else {
                    // Empty chunks go straight to READY (no mesh needed)
                    chunk.state = VoxelChunk.ChunkState.READY;
                }
            } catch (Exception e) {
                // Don't crash the worker thread — mark chunk as generated so it doesn't block
                chunk.state = VoxelChunk.ChunkState.GENERATED;
            }
        });
    }

    /**
     * Submit a mesh build task to the thread pool.
     */
    private void submitMesh(VoxelChunk chunk) {
        chunk.state = VoxelChunk.ChunkState.MESHING;
        chunkPool.submit(() -> {
            try {
                ChunkMesher.MeshBuildResult result = ChunkMesher.buildMesh(chunk);
                chunk.pendingMeshResult = result;
                uploadQueue.add(chunk);
            } catch (Exception e) {
                // On failure, allow retry
                chunk.state = VoxelChunk.ChunkState.GENERATED;
                needsMesh.add(chunk);
            }
        });
    }

    /**
     * Check if a chunk has all its existing neighbors generated (ready for meshing).
     * Null neighbors (outside loaded area) are fine — getBlockOrNeighbor returns AIR.
     */
    private boolean canMesh(VoxelChunk chunk) {
        for (int i = 0; i < 6; i++) {
            VoxelChunk neighbor = chunk.getNeighbor(i);
            if (neighbor != null && neighbor.state.ordinal() < VoxelChunk.ChunkState.GENERATED.ordinal()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Link all chunk neighbors. Called after loading new chunks.
     */
    private void linkAllNeighbors() {
        for (VoxelChunk chunk : chunks.values()) {
            linkNeighbor(chunk, VoxelChunk.NEIGHBOR_NEG_X, chunk.cx - 1, chunk.cy, chunk.cz);
            linkNeighbor(chunk, VoxelChunk.NEIGHBOR_POS_X, chunk.cx + 1, chunk.cy, chunk.cz);
            linkNeighbor(chunk, VoxelChunk.NEIGHBOR_NEG_Y, chunk.cx, chunk.cy - 1, chunk.cz);
            linkNeighbor(chunk, VoxelChunk.NEIGHBOR_POS_Y, chunk.cx, chunk.cy + 1, chunk.cz);
            linkNeighbor(chunk, VoxelChunk.NEIGHBOR_NEG_Z, chunk.cx, chunk.cy, chunk.cz - 1);
            linkNeighbor(chunk, VoxelChunk.NEIGHBOR_POS_Z, chunk.cx, chunk.cy, chunk.cz + 1);
        }
    }

    private void linkNeighbor(VoxelChunk chunk, int side, int ncx, int ncy, int ncz) {
        VoxelChunk neighbor = chunks.get(VoxelChunk.packKey(ncx, ncy, ncz));
        chunk.setNeighbor(side, neighbor);
    }

    /**
     * Render all visible chunk meshes into the G-buffer.
     * @return number of chunks rendered
     */
    public int render(Camera camera, MatrixStack matrices) {
        if (chunks.isEmpty()) return 0;

        // During shadow rendering, use the active depth shader instead of the G-buffer shader
        boolean shadowPass = WorldRenderer.isShadowRendering();
        Shader shader;

        if (shadowPass) {
            // The depth shader was already set up by LightRenderer — just use it
            shader = Shader.activeShader;
            // Bind an opaque dummy texture so the depth shader's alpha test passes
            // (depth_dir.glsl discards fragments with alpha < 0.01)
            shader.setTexture("textureSampler", Texture.MISSING, 0);
        } else {
            shader = Shaders.GBUFFER_VOXEL.getShader();
            shader.use();
            shader.setup(camera);

            // Bind all PBR texture arrays
            BlockTextureArray.bind(0);
            shader.setInt("blockTextures", 0);

            BlockTextureArray.bindNormal(1);
            shader.setInt("normalTextures", 1);

            BlockTextureArray.bindRoughness(2);
            shader.setInt("roughnessTextures", 2);

            BlockTextureArray.bindAO(3);
            shader.setInt("aoTextures", 3);
        }

        int count = 0;

        for (VoxelChunk chunk : chunks.values()) {
            // Skip empty or unmeshed chunks
            ChunkMesh mesh = chunk.getMesh();
            if (mesh == null || mesh.isFreed()) continue;

            // Frustum culling at chunk level
            if (!camera.isInsideFrustum(chunk.aabb)) continue;

            // Set model matrix (translate to chunk world position)
            modelMatrix.identity().translate(chunk.worldX(), chunk.worldY(), chunk.worldZ());

            shader.applyModelMatrix(modelMatrix);
            if (!shadowPass) {
                normalMatrix.identity();
                shader.applyNormalMatrix(normalMatrix);
            }

            mesh.render();
            count++;
        }

        if (!shadowPass) {
            BlockTextureArray.unbind(0);
            BlockTextureArray.unbind(1);
            BlockTextureArray.unbind(2);
            BlockTextureArray.unbind(3);
        }

        return count;
    }

    /**
     * Render water meshes for all visible chunks using the animated water shader.
     * Called separately from render() so water can use different GL state (blending, no backface culling).
     */
    public void renderWater(Camera camera, MatrixStack matrices, float worldTime) {
        if (chunks.isEmpty()) return;

        Shader shader = Shaders.GBUFFER_VOXEL_WATER.getShader();
        shader.use();
        shader.setup(camera);

        // Set water shader uniforms
        shader.setFloat("time", worldTime * 0.0003f);
        shader.setTexture("noiseTex", WaterRenderer.getNoiseTexture(), 0);

        // Enable blending for semi-transparent water
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        // Disable backface culling so water is visible from both sides
        glDisable(GL_CULL_FACE);

        for (VoxelChunk chunk : chunks.values()) {
            ChunkMesh waterMesh = chunk.getWaterMesh();
            if (waterMesh == null || waterMesh.isFreed()) continue;

            // Frustum culling
            if (!camera.isInsideFrustum(chunk.aabb)) continue;

            modelMatrix.identity().translate(chunk.worldX(), chunk.worldY(), chunk.worldZ());
            normalMatrix.identity();

            shader.applyModelMatrix(modelMatrix);
            shader.applyNormalMatrix(normalMatrix);

            waterMesh.render();
        }

        // Restore GL state
        glEnable(GL_CULL_FACE);
        glDisable(GL_BLEND);
    }

    /**
     * Get the block type at a world position.
     */
    public BlockType getBlockAt(int wx, int wy, int wz) {
        int cx = Math.floorDiv(wx, VoxelChunk.SIZE);
        int cy = Math.floorDiv(wy, VoxelChunk.SIZE);
        int cz = Math.floorDiv(wz, VoxelChunk.SIZE);

        VoxelChunk chunk = chunks.get(VoxelChunk.packKey(cx, cy, cz));
        if (chunk == null) return BlockType.AIR;

        int lx = Math.floorMod(wx, VoxelChunk.SIZE);
        int ly = Math.floorMod(wy, VoxelChunk.SIZE);
        int lz = Math.floorMod(wz, VoxelChunk.SIZE);

        return chunk.getBlock(lx, ly, lz);
    }

    /**
     * Set a block at a world position and mark the chunk dirty.
     */
    public void setBlockAt(int wx, int wy, int wz, BlockType type) {
        int cx = Math.floorDiv(wx, VoxelChunk.SIZE);
        int cy = Math.floorDiv(wy, VoxelChunk.SIZE);
        int cz = Math.floorDiv(wz, VoxelChunk.SIZE);

        VoxelChunk chunk = chunks.get(VoxelChunk.packKey(cx, cy, cz));
        if (chunk == null) return;

        int lx = Math.floorMod(wx, VoxelChunk.SIZE);
        int ly = Math.floorMod(wy, VoxelChunk.SIZE);
        int lz = Math.floorMod(wz, VoxelChunk.SIZE);

        chunk.setBlock(lx, ly, lz, type);

        // Schedule remesh for this chunk
        scheduleRemesh(chunk);

        // Also mark adjacent chunks dirty if block is on the boundary
        if (lx == 0) markNeighborDirty(cx - 1, cy, cz);
        if (lx == VoxelChunk.SIZE - 1) markNeighborDirty(cx + 1, cy, cz);
        if (ly == 0) markNeighborDirty(cx, cy - 1, cz);
        if (ly == VoxelChunk.SIZE - 1) markNeighborDirty(cx, cy + 1, cz);
        if (lz == 0) markNeighborDirty(cx, cy, cz - 1);
        if (lz == VoxelChunk.SIZE - 1) markNeighborDirty(cx, cy, cz + 1);
    }

    /**
     * Schedule a chunk for async re-meshing.
     */
    private void scheduleRemesh(VoxelChunk chunk) {
        chunk.markDirty();
        VoxelChunk.ChunkState s = chunk.state;
        if (s == VoxelChunk.ChunkState.READY || s == VoxelChunk.ChunkState.GENERATED) {
            chunk.state = VoxelChunk.ChunkState.GENERATED;
            needsMesh.add(chunk);
        }
        // If currently MESHING, the upload handler will detect the dirty flag and re-queue
    }

    private void markNeighborDirty(int cx, int cy, int cz) {
        VoxelChunk neighbor = chunks.get(VoxelChunk.packKey(cx, cy, cz));
        if (neighbor != null) {
            scheduleRemesh(neighbor);
        }
    }

    /**
     * Check if a world-space AABB intersects any solid block.
     * Used for entity collision.
     */
    public boolean hasCollision(AABB box) {
        int minX = (int) Math.floor(box.minX());
        int minY = (int) Math.floor(box.minY());
        int minZ = (int) Math.floor(box.minZ());
        int maxX = (int) Math.floor(box.maxX());
        int maxY = (int) Math.floor(box.maxY());
        int maxZ = (int) Math.floor(box.maxZ());

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockType block = getBlockAt(x, y, z);
                    if (block.solid) return true;
                }
            }
        }
        return false;
    }

    /**
     * Get all solid block AABBs that intersect a region.
     * Used for physics collision resolution.
     */
    public List<AABB> getCollisionBoxes(AABB region) {
        List<AABB> boxes = new ArrayList<>();

        int minX = (int) Math.floor(region.minX());
        int minY = (int) Math.floor(region.minY());
        int minZ = (int) Math.floor(region.minZ());
        int maxX = (int) Math.floor(region.maxX());
        int maxY = (int) Math.floor(region.maxY());
        int maxZ = (int) Math.floor(region.maxZ());

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockType block = getBlockAt(x, y, z);
                    if (block.solid) {
                        boxes.add(new AABB(x, y, z, x + 1, y + 1, z + 1));
                    }
                }
            }
        }

        return boxes;
    }

    /**
     * DDA voxel raycast through the block grid.
     * Returns the hit result or null if no block hit within maxDistance.
     */
    public VoxelRaycastResult raycast(Vector3f origin, Vector3f direction, float maxDistance) {
        // Normalize direction
        float dirX = direction.x;
        float dirY = direction.y;
        float dirZ = direction.z;
        float len = (float) Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
        if (len < 1e-8f) return null;
        dirX /= len;
        dirY /= len;
        dirZ /= len;

        // Current voxel
        int x = (int) Math.floor(origin.x);
        int y = (int) Math.floor(origin.y);
        int z = (int) Math.floor(origin.z);

        // Step direction
        int stepX = dirX > 0 ? 1 : (dirX < 0 ? -1 : 0);
        int stepY = dirY > 0 ? 1 : (dirY < 0 ? -1 : 0);
        int stepZ = dirZ > 0 ? 1 : (dirZ < 0 ? -1 : 0);

        // Distance to next voxel boundary in each axis
        float tMaxX = dirX != 0 ? ((stepX > 0 ? (x + 1 - origin.x) : (origin.x - x)) / Math.abs(dirX)) : Float.MAX_VALUE;
        float tMaxY = dirY != 0 ? ((stepY > 0 ? (y + 1 - origin.y) : (origin.y - y)) / Math.abs(dirY)) : Float.MAX_VALUE;
        float tMaxZ = dirZ != 0 ? ((stepZ > 0 ? (z + 1 - origin.z) : (origin.z - z)) / Math.abs(dirZ)) : Float.MAX_VALUE;

        // Distance between voxel boundaries
        float tDeltaX = dirX != 0 ? (1.0f / Math.abs(dirX)) : Float.MAX_VALUE;
        float tDeltaY = dirY != 0 ? (1.0f / Math.abs(dirY)) : Float.MAX_VALUE;
        float tDeltaZ = dirZ != 0 ? (1.0f / Math.abs(dirZ)) : Float.MAX_VALUE;

        float distance = 0;
        int normalX = 0, normalY = 0, normalZ = 0;

        while (distance < maxDistance) {
            BlockType block = getBlockAt(x, y, z);
            if (block.solid) {
                Vector3f hitPos = new Vector3f(
                        origin.x + dirX * distance,
                        origin.y + dirY * distance,
                        origin.z + dirZ * distance
                );
                Vector3f normal = new Vector3f(normalX, normalY, normalZ);
                return new VoxelRaycastResult(x, y, z, block, hitPos, normal, distance);
            }

            // Advance to next voxel
            if (tMaxX < tMaxY) {
                if (tMaxX < tMaxZ) {
                    x += stepX;
                    distance = tMaxX;
                    tMaxX += tDeltaX;
                    normalX = -stepX; normalY = 0; normalZ = 0;
                } else {
                    z += stepZ;
                    distance = tMaxZ;
                    tMaxZ += tDeltaZ;
                    normalX = 0; normalY = 0; normalZ = -stepZ;
                }
            } else {
                if (tMaxY < tMaxZ) {
                    y += stepY;
                    distance = tMaxY;
                    tMaxY += tDeltaY;
                    normalX = 0; normalY = -stepY; normalZ = 0;
                } else {
                    z += stepZ;
                    distance = tMaxZ;
                    tMaxZ += tDeltaZ;
                    normalX = 0; normalY = 0; normalZ = -stepZ;
                }
            }
        }

        return null;
    }

    /**
     * Free all chunks and GPU resources. Shuts down the worker thread pool.
     */
    public void free() {
        chunkPool.shutdownNow();
        try {
            chunkPool.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {}

        uploadQueue.clear();
        needsMesh.clear();

        for (VoxelChunk chunk : chunks.values()) {
            chunk.unlinkNeighbors();
            chunk.free();
        }
        chunks.clear();
    }

    public int getLoadedChunkCount() {
        return chunks.size();
    }

    public int getMeshedChunkCount() {
        int count = 0;
        for (VoxelChunk chunk : chunks.values()) {
            if (chunk.getMesh() != null && !chunk.getMesh().isFreed()) count++;
        }
        return count;
    }

    public int getPendingGenerations() {
        int count = 0;
        for (VoxelChunk chunk : chunks.values()) {
            VoxelChunk.ChunkState s = chunk.state;
            if (s == VoxelChunk.ChunkState.CREATED || s == VoxelChunk.ChunkState.GENERATING) count++;
        }
        return count;
    }

    public int getPendingMeshes() {
        return needsMesh.size() + uploadQueue.size();
    }

    public int getRenderDistanceChunks() {
        return renderDistanceChunks;
    }

    public void setRenderDistanceChunks(int distance) {
        this.renderDistanceChunks = distance;
    }

    /**
     * Result of a voxel raycast.
     */
    public record VoxelRaycastResult(
            int blockX, int blockY, int blockZ,
            BlockType blockType,
            Vector3f hitPos,
            Vector3f normal,
            float distance
    ) {}
}
