package cinnamon.world.world;

import cinnamon.Client;
import cinnamon.registry.MaterialRegistry;
import cinnamon.registry.TerrainRegistry;
import cinnamon.render.Camera;
import cinnamon.render.MatrixStack;
import cinnamon.render.WorldRenderer;
import cinnamon.utils.AABB;
import cinnamon.world.collisions.CollisionDetector;
import cinnamon.world.collisions.CollisionResult;
import cinnamon.world.collisions.Hit;
import cinnamon.world.entity.living.LocalPlayer;
import cinnamon.world.light.DirectionalLight;
import cinnamon.world.terrain.Terrain;
import cinnamon.world.voxel.*;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * A high-performance voxel world using chunk-based storage and rendering.
 * <p>
 * Blocks are stored as integer IDs in flat arrays (no per-block Java objects).
 * Chunks are 16x16x16 with greedy-meshed GPU buffers.
 * Rendering feeds directly into the PBR deferred G-buffer pipeline.
 * Chunks are dynamically loaded/unloaded around the player.
 */
public class VoxelWorld extends WorldClient {

    private final ChunkManager chunkManager;
    private final long worldSeed;

    public VoxelWorld() {
        this(System.nanoTime());
    }

    public VoxelWorld(long seed) {
        this.worldSeed = seed;
        this.chunkManager = new ChunkManager(10);
    }

    @Override
    public void init() {
        // Initialize noise with seed
        SimplexNoise.seed(worldSeed);

        // Initialize block texture array (must happen after materials are loaded)
        BlockTextureArray.init();

        // Set client
        client = Client.getInstance();
        client.setScreen(null);
        client.world = this;

        // Init hud
        hud.init();

        // Sunlight with shadows
        sunLight.castsShadows(true);
        ((DirectionalLight) sunLight).intensity(1.0f);
        addLight(sunLight);

        // Set world time to morning
        worldTime = 2000;

        // Create player and spawn above terrain
        respawnVoxel();

        // Do initial chunk loading around spawn
        chunkManager.update(player.getPos());

        // Process several rounds to have chunks ready at spawn
        for (int i = 0; i < 40; i++) {
            chunkManager.update(player.getPos());
        }

        runScheduledTicks();
    }

    @Override
    protected void tempLoad() {
        // Override to prevent the old terrain generation from running
        // The voxel world generates terrain through ChunkManager
    }

    private void respawnVoxel() {
        player = new LocalPlayer();
        // Spawn above the expected terrain height
        player.setPos(0.5f, 70f, 0.5f);
        player.getAbilities().godMode(false).canFly(true);
        this.addEntity(player);

        cinnamon.animation.Animation anim = player.getAnimation("blink");
        if (anim != null)
            anim.setLoop(cinnamon.animation.Animation.Loop.LOOP).play();
    }

    @Override
    public void respawn(boolean init) {
        respawnVoxel();
    }

    @Override
    public void tick() {
        super.tick();

        // Update chunk loading/unloading based on player position
        if (player != null && !isPaused()) {
            chunkManager.update(player.getPos());
        }
    }

    // ---- Rendering ---- //

    @Override
    public int renderTerrain(Camera camera, MatrixStack matrices, float delta) {
        // Render voxel chunks instead of the old per-block terrain
        return chunkManager.render(camera, matrices);
    }

    @Override
    public void renderWater(Camera camera, MatrixStack matrices, float delta) {
        // Render animated water on voxel water block faces instead of the old flat plane
        chunkManager.renderWater(camera, matrices, worldTime + delta);
    }

    @Override
    public void scroll(double x, double y) {
        // In VoxelWorld, scroll cycles selected block type instead of inventory
        if (player instanceof LocalPlayer lp) {
            int dir = y > 0 ? -1 : y < 0 ? 1 : 0;
            lp.scrollBlockType(dir);
        }
    }

    // ---- Collision / Terrain Queries ---- //

    /**
     * Override terrain query to return voxel block collision boxes.
     * This is called by PhysEntity for collision resolution.
     */
    @Override
    public List<Terrain> getTerrains(AABB region) {
        // Return empty — voxel collision is handled through the block grid
        // We use VoxelBlockTerrains for compatibility
        return getVoxelTerrains(region);
    }

    /**
     * Create lightweight Terrain proxies for voxel blocks in a region.
     * These are temporary objects only used for collision, not stored permanently.
     */
    private List<Terrain> getVoxelTerrains(AABB region) {
        List<Terrain> result = new ArrayList<>();

        int minX = (int) Math.floor(region.minX());
        int minY = (int) Math.floor(region.minY());
        int minZ = (int) Math.floor(region.minZ());
        int maxX = (int) Math.floor(region.maxX());
        int maxY = (int) Math.floor(region.maxY());
        int maxZ = (int) Math.floor(region.maxZ());

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockType block = chunkManager.getBlockAt(x, y, z);
                    if (block.solid) {
                        result.add(new VoxelBlockTerrain(x, y, z, block));
                    }
                }
            }
        }

        return result;
    }

    /**
     * Override terrain raycast to use DDA voxel raycasting.
     */
    @Override
    public Hit<Terrain> raycastTerrain(AABB area, Vector3f pos, Vector3f dirLen, Predicate<Terrain> predicate) {
        // Use DDA raycast for fast voxel traversal
        float maxDist = dirLen.length();
        if (maxDist < 1e-6f) return null;

        Vector3f dir = new Vector3f(dirLen).normalize();
        ChunkManager.VoxelRaycastResult hit = chunkManager.raycast(pos, dir, maxDist);

        if (hit == null) return null;

        // Create a terrain proxy for the hit block
        VoxelBlockTerrain terrain = new VoxelBlockTerrain(hit.blockX(), hit.blockY(), hit.blockZ(), hit.blockType());

        // Test the predicate
        if (!predicate.test(terrain)) return null;

        // Build CollisionResult for compatibility
        CollisionResult collResult = CollisionDetector.collisionRay(
                terrain.getAABB(), pos, dirLen
        );

        if (collResult == null) {
            // Fallback: construct collision result manually from DDA hit
            collResult = new CollisionResult(
                    hit.distance() / maxDist,
                    hit.normal(),
                    hit.hitPos()
            );
        }

        return new Hit<>(collResult, terrain);
    }

    // ---- Voxel-specific API ---- //

    public ChunkManager getChunkManager() {
        return chunkManager;
    }

    public BlockType getBlockAt(int x, int y, int z) {
        return chunkManager.getBlockAt(x, y, z);
    }

    public void setBlockAt(int x, int y, int z, BlockType type) {
        chunkManager.setBlockAt(x, y, z, type);
    }

    public long getWorldSeed() {
        return worldSeed;
    }

    @Override
    public void close() {
        super.close();
        chunkManager.free();
        BlockTextureArray.free();
    }

    // ---- Lightweight Terrain proxy for collision compatibility ---- //

    /**
     * A minimal Terrain subclass that wraps a single voxel block position.
     * Used only for collision queries — no model, no rendering, no storage.
     * Created on-the-fly during collision checks and immediately discarded.
     */
    private static class VoxelBlockTerrain extends Terrain {

        private static final List<AABB> SINGLE_BOX = new ArrayList<>();
        private final BlockType blockType;

        VoxelBlockTerrain(int x, int y, int z, BlockType type) {
            super(null, TerrainRegistry.BOX);
            this.blockType = type;

            // Set position and AABB directly
            this.pos.set(x, y, z);
            this.aabb.set(x, y, z, x + 1, y + 1, z + 1);

            // Set precise AABB
            this.preciseAABB.clear();
            this.preciseAABB.add(new AABB(x, y, z, x + 1, y + 1, z + 1));
        }

        @Override
        public void render(Camera camera, MatrixStack matrices, float delta) {
            // Never rendered — voxel chunks handle rendering
        }

        @Override
        protected void updateAABB() {
            // No-op for proxy terrain
        }

        @Override
        public boolean shouldRender(Camera camera) {
            return false;
        }
    }
}
