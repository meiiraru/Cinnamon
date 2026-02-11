package cinnamon.world.voxel;

import cinnamon.utils.AABB;

/**
 * A 16x16x16 voxel chunk stored as a flat short array.
 * Each value is a BlockType ordinal. ID 0 = AIR.
 * <p>
 * Index layout: index = x + z * SIZE + y * SIZE * SIZE
 * <p>
 * Memory: 16*16*16 * 2 bytes = 8 KB per chunk (vs hundreds of KB with per-block objects).
 */
public class VoxelChunk {

    /** Lifecycle state for async generation and meshing. */
    public enum ChunkState {
        CREATED,      // Just allocated, no block data yet
        GENERATING,   // Generation task submitted to thread pool
        GENERATED,    // Block data populated, waiting for mesh
        MESHING,      // Mesh build task submitted to thread pool
        READY         // Mesh uploaded to GPU, fully usable
    }

    public static final int SIZE = 16;
    public static final int SIZE_SQ = SIZE * SIZE;
    public static final int VOLUME = SIZE * SIZE * SIZE;

    /** Chunk grid coordinates (world pos = cx*16, cy*16, cz*16) */
    public final int cx, cy, cz;

    /** World-space AABB for frustum culling */
    public final AABB aabb;

    /** Block data — each short is a BlockType ordinal */
    private final short[] blocks = new short[VOLUME];

    /** Current lifecycle state (volatile for cross-thread visibility) */
    public volatile ChunkState state = ChunkState.CREATED;

    /** Mesh data built by a worker thread, waiting for GL upload on the main thread */
    public volatile ChunkMesher.MeshBuildResult pendingMeshResult;

    /** Whether this chunk needs remeshing */
    private volatile boolean dirty = true;

    /** Whether this chunk has any non-air blocks */
    private boolean empty = true;

    /** Number of non-air blocks */
    private int blockCount = 0;

    /** The GPU mesh for this chunk (null until meshed) */
    private ChunkMesh mesh;

    /** The GPU mesh for water faces in this chunk (null if no water) */
    private ChunkMesh waterMesh;

    /** References to the 6 neighboring chunks (for cross-boundary face culling) */
    // Order: -X, +X, -Y, +Y, -Z, +Z
    private final VoxelChunk[] neighbors = new VoxelChunk[6];

    public static final int NEIGHBOR_NEG_X = 0;
    public static final int NEIGHBOR_POS_X = 1;
    public static final int NEIGHBOR_NEG_Y = 2;
    public static final int NEIGHBOR_POS_Y = 3;
    public static final int NEIGHBOR_NEG_Z = 4;
    public static final int NEIGHBOR_POS_Z = 5;

    public VoxelChunk(int cx, int cy, int cz) {
        this.cx = cx;
        this.cy = cy;
        this.cz = cz;

        float wx = cx * SIZE;
        float wy = cy * SIZE;
        float wz = cz * SIZE;
        this.aabb = new AABB(wx, wy, wz, wx + SIZE, wy + SIZE, wz + SIZE);
    }

    // ---- Block Access ---- //

    private static int index(int x, int y, int z) {
        return x + z * SIZE + y * SIZE_SQ;
    }

    /**
     * Get block type at local coordinates (0-15).
     */
    public BlockType getBlock(int x, int y, int z) {
        if (x < 0 || x >= SIZE || y < 0 || y >= SIZE || z < 0 || z >= SIZE)
            return BlockType.AIR;
        return BlockType.fromId(blocks[index(x, y, z)]);
    }

    /**
     * Get block type, checking neighbors for out-of-bounds coordinates.
     * Used by the mesher for cross-boundary face culling.
     */
    public BlockType getBlockOrNeighbor(int x, int y, int z) {
        if (x >= 0 && x < SIZE && y >= 0 && y < SIZE && z >= 0 && z < SIZE)
            return BlockType.fromId(blocks[index(x, y, z)]);

        // Determine which neighbor to check
        VoxelChunk neighbor = null;
        int nx = x, ny = y, nz = z;

        if (x < 0) { neighbor = neighbors[NEIGHBOR_NEG_X]; nx = x + SIZE; }
        else if (x >= SIZE) { neighbor = neighbors[NEIGHBOR_POS_X]; nx = x - SIZE; }
        else if (y < 0) { neighbor = neighbors[NEIGHBOR_NEG_Y]; ny = y + SIZE; }
        else if (y >= SIZE) { neighbor = neighbors[NEIGHBOR_POS_Y]; ny = y - SIZE; }
        else if (z < 0) { neighbor = neighbors[NEIGHBOR_NEG_Z]; nz = z + SIZE; }
        else if (z >= SIZE) { neighbor = neighbors[NEIGHBOR_POS_Z]; nz = z - SIZE; }

        if (neighbor == null) return BlockType.AIR;
        return neighbor.getBlock(nx, ny, nz);
    }

    /**
     * Set block type at local coordinates. Marks chunk dirty.
     */
    public void setBlock(int x, int y, int z, BlockType type) {
        if (x < 0 || x >= SIZE || y < 0 || y >= SIZE || z < 0 || z >= SIZE) return;

        int idx = index(x, y, z);
        short oldId = blocks[idx];
        short newId = (short) type.ordinal();

        if (oldId == newId) return;

        // Update block count
        if (oldId == 0 && newId != 0) blockCount++;
        else if (oldId != 0 && newId == 0) blockCount--;

        blocks[idx] = newId;
        empty = blockCount == 0;
        dirty = true;
    }

    /**
     * Bulk set without per-block dirty marking. Call markDirty() after.
     */
    public void setBlockFast(int x, int y, int z, BlockType type) {
        if (x < 0 || x >= SIZE || y < 0 || y >= SIZE || z < 0 || z >= SIZE) return;
        int idx = index(x, y, z);
        short oldId = blocks[idx];
        short newId = (short) type.ordinal();
        if (oldId == newId) return;

        if (oldId == 0 && newId != 0) blockCount++;
        else if (oldId != 0 && newId == 0) blockCount--;

        blocks[idx] = newId;
    }

    /**
     * Finalize after bulk setBlockFast calls.
     */
    public void finishBulkSet() {
        empty = blockCount == 0;
        dirty = true;
    }

    /**
     * Get raw block ID at local coordinates.
     */
    public short getBlockId(int x, int y, int z) {
        if (x < 0 || x >= SIZE || y < 0 || y >= SIZE || z < 0 || z >= SIZE) return 0;
        return blocks[index(x, y, z)];
    }

    // ---- Neighbor Management ---- //

    public void setNeighbor(int side, VoxelChunk chunk) {
        neighbors[side] = chunk;
    }

    public VoxelChunk getNeighbor(int side) {
        return neighbors[side];
    }

    /**
     * Unlink this chunk from all neighbors.
     */
    public void unlinkNeighbors() {
        for (int i = 0; i < 6; i++) {
            if (neighbors[i] != null) {
                // Remove ourselves from their neighbor list
                int opposite = i ^ 1; // 0<->1, 2<->3, 4<->5
                neighbors[i].neighbors[opposite] = null;
                neighbors[i] = null;
            }
        }
    }

    // ---- State ---- //

    public boolean isDirty() { return dirty; }
    public void markDirty() { dirty = true; }
    public void clearDirty() { dirty = false; }

    public boolean isEmpty() { return empty; }
    public int getBlockCount() { return blockCount; }

    public ChunkMesh getMesh() { return mesh; }

    public void setMesh(ChunkMesh mesh) {
        // Free old mesh if exists
        if (this.mesh != null) {
            this.mesh.free();
        }
        this.mesh = mesh;
    }

    public ChunkMesh getWaterMesh() { return waterMesh; }

    public void setWaterMesh(ChunkMesh waterMesh) {
        // Free old water mesh if exists
        if (this.waterMesh != null) {
            this.waterMesh.free();
        }
        this.waterMesh = waterMesh;
    }

    /**
     * World-space origin of this chunk.
     */
    public float worldX() { return cx * SIZE; }
    public float worldY() { return cy * SIZE; }
    public float worldZ() { return cz * SIZE; }

    /**
     * Free GPU resources.
     */
    public void free() {
        if (mesh != null) {
            mesh.free();
            mesh = null;
        }
        if (waterMesh != null) {
            waterMesh.free();
            waterMesh = null;
        }
    }

    /**
     * Pack chunk coordinates into a single long key for HashMap storage.
     */
    public static long packKey(int cx, int cy, int cz) {
        // Pack into 64 bits: 21 bits each for x,z (signed), 22 bits for y (signed)
        return ((long) (cx & 0x1FFFFF)) |
               ((long) (cy & 0x3FFFFF) << 21) |
               ((long) (cz & 0x1FFFFF) << 43);
    }

    public long getKey() {
        return packKey(cx, cy, cz);
    }
}
