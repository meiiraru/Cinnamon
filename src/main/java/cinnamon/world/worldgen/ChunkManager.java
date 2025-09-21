package cinnamon.world.worldgen;

import cinnamon.Client;
import cinnamon.registry.MaterialRegistry;
import cinnamon.registry.TerrainRegistry;
import cinnamon.utils.PerlinNoise2D;
import cinnamon.world.terrain.Terrain;
import cinnamon.world.world.World;
import cinnamon.world.worldgen.chunk.Chunk;
import cinnamon.world.worldgen.io.BlockSpec;
import cinnamon.world.worldgen.io.WorldIO;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Streams chunks around a center, handles generation and IO, and maps cells to Terrain.
 */
public class ChunkManager {

    public static final int CHUNK_SIZE = Chunk.CHUNK_SIZE;

    private final World world;
    private final String worldName;
    private final long seed;
    private final WorldIO.GeneratorConfig genCfg;

    private final Map<Long, LoadedChunk> loaded = new ConcurrentHashMap<>();
    private final Set<Long> dirty = Collections.synchronizedSet(new HashSet<>());
    private final Set<Long> loading = Collections.synchronizedSet(new HashSet<>());
    private final Queue<WorldIO.ChunkData> ready = new ConcurrentLinkedQueue<>();
    private final ExecutorService ioPool = Executors.newFixedThreadPool(Math.max(1, Runtime.getRuntime().availableProcessors() / 2));

    // generation helpers
    private final PerlinNoise2D noise;
    private final int waterLevel; // from generator config
    private int materializeBudgetPerTick = 2;

    public ChunkManager(World world, String worldName, long seed) {
        this(world, worldName, seed, defaultGenerator());
    }

    public ChunkManager(World world, String worldName, long seed, WorldIO.GeneratorConfig cfg) {
        this.world = world;
        this.worldName = worldName;
        this.seed = seed;
        this.genCfg = cfg == null ? defaultGenerator() : cfg;
        this.noise = new PerlinNoise2D(seed);
        this.waterLevel = this.genCfg.waterLevel;
    }

    private static WorldIO.GeneratorConfig defaultGenerator() {
        WorldIO.GeneratorConfig g = new WorldIO.GeneratorConfig();
        g.type = "default";
        g.octaves = 4; g.scale = 0.01f; g.lacunarity = 2f; g.persistence = 0.5f; g.amplitude = 10f; g.baseHeight = 12f; g.waterLevel = 16;
        return g;
    }

    public void setMaterializeBudgetPerTick(int budget) { this.materializeBudgetPerTick = Math.max(1, budget); }

    private static long key(int cx, int cy, int cz) {
        return (((long) (cx) & 0x1FFFFFL) << 42) | (((long) (cy) & 0x1FFFFFL) << 21) | ((long) (cz) & 0x1FFFFFL);
    }

    public void ensureRadius(int centerX, int centerY, int centerZ, int radiusXY, int radiusY) {
        // compute coordinates within radius and sort by distance to prioritize nearby chunks
        record Pos(int cx, int cy, int cz, int d2) {}

        List<Pos> order = new ArrayList<>();
        for (int dx = -radiusXY; dx <= radiusXY; dx++)
            for (int dz = -radiusXY; dz <= radiusXY; dz++)
                for (int dy = -radiusY; dy <= radiusY; dy++) {
                    int cx = centerX + dx;
                    int cy = centerY + dy;
                    int cz = centerZ + dz;
                    int d2 = dx * dx + dy * dy + dz * dz;
                    order.add(new Pos(cx, cy, cz, d2));
                }

        order.sort(Comparator.comparingInt(Pos::d2));

        // load/generate in sorted order
        Set<Long> wanted = new HashSet<>();
        for (Pos p : order) {
            long k = key(p.cx, p.cy, p.cz);
            wanted.add(k);
            asyncLoadIfAbsent(p.cx, p.cy, p.cz);
        }

        // unload others
        for (long k : new ArrayList<>(loaded.keySet()))
            if (!wanted.contains(k)) unload(k);
    }

    public void tick() {
        // materialize a few ready chunks per tick to amortize work on main thread
    int budget = this.materializeBudgetPerTick;
        for (int i = 0; i < budget; i++) {
            WorldIO.ChunkData data = ready.poll();
            if (data == null) break;
            long k = key(data.cx, data.cy, data.cz);
            loading.remove(k);
            LoadedChunk lc = materialize(data);
            loaded.put(k, lc);
            // now that it's in the map, update seam masks with any loaded neighbors
            updateSeamsAround(data.cx, data.cy, data.cz);
        }

        // autosave periodically (simple: save all dirty every few seconds)
        if (Client.getInstance().ticks % 600 == 0) saveAllDirty();
    }

    public void saveAllDirty() {
        synchronized (dirty) {
            for (long k : new ArrayList<>(dirty)) {
                LoadedChunk lc = loaded.get(k);
                if (lc != null) saveChunk(lc);
                dirty.remove(k);
            }
        }
    }

    private void unload(long k) {
        LoadedChunk lc = loaded.remove(k);
        if (lc == null) return;
        // remove terrains from world
        for (Terrain t : lc.terrains) world.removeTerrain(t);
        if (lc.dirty) saveChunk(lc);
    }

    private void asyncLoadIfAbsent(int cx, int cy, int cz) {
        long k = key(cx, cy, cz);
        if (loaded.containsKey(k) || loading.contains(k)) return;
        loading.add(k);
        ioPool.submit(() -> {
            WorldIO.ChunkData data = WorldIO.loadChunk(worldName, cx, cy, cz);
            if (data == null) data = generateChunk(cx, cy, cz);
            ready.add(data);
        });
    }

    private LoadedChunk materialize(WorldIO.ChunkData data) {
        LoadedChunk lc = new LoadedChunk();
        lc.data = data; lc.dirty = false;
        lc.terrainByIndex = new Terrain[CHUNK_SIZE * CHUNK_SIZE * CHUNK_SIZE];

        // index 0 = air
        for (int x = 0; x < CHUNK_SIZE; x++)
            for (int y = 0; y < CHUNK_SIZE; y++)
                for (int z = 0; z < CHUNK_SIZE; z++) {
                    int flat = (x * CHUNK_SIZE + y) * CHUNK_SIZE + z;
                    int idx = data.data[flat] & 0xFF;
                    if (idx == 0) continue;
                    BlockSpec spec = data.palette[idx];
                    Terrain t = spec.terrain.getFactory().get();
                    t.setMaterial(spec.material);
                    t.setRotation(spec.rot);
                    t.setPos(x + data.cx * CHUNK_SIZE, y + data.cy * CHUNK_SIZE, z + data.cz * CHUNK_SIZE);
                    // compute face mask with current neighbors (may refine after neighbors load)
                    byte mask = computeMaskFor(lc, x, y, z);
                    t.setFaceMask(mask);
                    world.addTerrain(t);
                    lc.terrains.add(t);
                    lc.terrainByIndex[flat] = t;
                }

        return lc;
    }

    private void saveChunk(LoadedChunk lc) {
        WorldIO.ChunkData snapshot = lc.data; // immutable arrays used, safe enough for now
        ioPool.submit(() -> WorldIO.saveChunk(worldName, snapshot));
    }

    private WorldIO.ChunkData generateChunk(int cx, int cy, int cz) {
        WorldIO.ChunkData cd = new WorldIO.ChunkData();
        cd.cx = cx; cd.cy = cy; cd.cz = cz;
        cd.seedHash = seed ^ (((long) cx) << 32) ^ cz;

        // palette: 0=air, 1=grass, 2=dirt, 3=stone, 4=sand, 5=water, 6=snow
        cd.palette = new BlockSpec[]{
                null,
                new BlockSpec(TerrainRegistry.BOX, MaterialRegistry.GRASS, (byte) 0),
                new BlockSpec(TerrainRegistry.BOX, MaterialRegistry.PLYWOOD, (byte) 0), // use as DIRT placeholder if no DIRT material
                new BlockSpec(TerrainRegistry.BOX, MaterialRegistry.GRANITE, (byte) 0),
                new BlockSpec(TerrainRegistry.BOX, MaterialRegistry.SAND, (byte) 0),
                new BlockSpec(TerrainRegistry.BOX, MaterialRegistry.WATER, (byte) 0),
                new BlockSpec(TerrainRegistry.BOX, MaterialRegistry.SNOW, (byte) 0)
        };

        cd.data = new byte[CHUNK_SIZE * CHUNK_SIZE * CHUNK_SIZE];

        // world-space base offsets
        int baseX = cx * CHUNK_SIZE;
        int baseY = cy * CHUNK_SIZE;
        int baseZ = cz * CHUNK_SIZE;

        for (int x = 0; x < CHUNK_SIZE; x++) {
            for (int z = 0; z < CHUNK_SIZE; z++) {
                int wx = baseX + x;
                int wz = baseZ + z;

                float h = heightAt(wx, wz);
                int heightY = Math.round(h);

                for (int y = 0; y < CHUNK_SIZE; y++) {
                    int wy = baseY + y;
                    int index = (x * CHUNK_SIZE + y) * CHUNK_SIZE + z;

                    byte id = 0; // air
                    if (wy <= heightY) {
                        if (wy == heightY) {
                            // surface material
                            if (heightY < waterLevel + 1) id = 4; // sand shore
                            else if (heightY > waterLevel + 20) id = 6; // snow
                            else id = 1; // grass
                        } else if (wy >= heightY - 3) {
                            id = 2; // dirt-like layer
                        } else {
                            id = 3; // stone
                        }
                    } else if (wy <= waterLevel) {
                        id = 5; // water fill
                    }

                    cd.data[index] = id;
                }
            }
        }

        return cd;
    }

    private float heightAt(int x, int z) {
    float n = noise.fbm(x * genCfg.scale, z * genCfg.scale, genCfg.octaves, genCfg.lacunarity, genCfg.persistence); // [-1,1] approx
    float h = genCfg.baseHeight + n * genCfg.amplitude; // base + amplitude
        return h;
    }

    private static class LoadedChunk {
        WorldIO.ChunkData data;
        boolean dirty;
        List<Terrain> terrains = new ArrayList<>();
    Terrain[] terrainByIndex; // length = CHUNK_SIZE^3, null for air
    }

    public void shutdown() {
        try {
            ioPool.shutdownNow();
        } catch (Exception ignored) {}
    }

    // ---- face mask helpers ----

    private byte computeMaskFor(LoadedChunk lc, int x, int y, int z) {
        byte mask = 0;
        if (isSolidNeighbor(lc, x + 1, y, z,  1, 0, 0)) mask |= 1 << 0; // +X
        if (isSolidNeighbor(lc, x - 1, y, z, -1, 0, 0)) mask |= 1 << 1; // -X
        if (isSolidNeighbor(lc, x, y + 1, z,  0, 1, 0)) mask |= 1 << 2; // +Y
        if (isSolidNeighbor(lc, x, y - 1, z,  0,-1, 0)) mask |= 1 << 3; // -Y
        if (isSolidNeighbor(lc, x, y, z + 1,  0, 0, 1)) mask |= 1 << 4; // +Z
        if (isSolidNeighbor(lc, x, y, z - 1,  0, 0,-1)) mask |= 1 << 5; // -Z
        return mask;
    }

    private boolean isSolidNeighbor(LoadedChunk lc, int nx, int ny, int nz, int dx, int dy, int dz) {
        // within same chunk bounds
        if (nx >= 0 && nx < CHUNK_SIZE && ny >= 0 && ny < CHUNK_SIZE && nz >= 0 && nz < CHUNK_SIZE) {
            int nFlat = (nx * CHUNK_SIZE + ny) * CHUNK_SIZE + nz;
            return (lc.data.data[nFlat] & 0xFF) != 0;
        }

        // need to look into neighbor chunk
        int ncx = lc.data.cx + Integer.signum(dx);
        int ncy = lc.data.cy + Integer.signum(dy);
        int ncz = lc.data.cz + Integer.signum(dz);
        long nk = key(ncx, ncy, ncz);
        LoadedChunk nb = loaded.get(nk);
        if (nb == null) return false; // neighbor not loaded yet -> don't hide

        int lx = nx; int ly = ny; int lz = nz;
        if (lx < 0) lx = CHUNK_SIZE - 1; else if (lx >= CHUNK_SIZE) lx = 0;
        if (ly < 0) ly = CHUNK_SIZE - 1; else if (ly >= CHUNK_SIZE) ly = 0;
        if (lz < 0) lz = CHUNK_SIZE - 1; else if (lz >= CHUNK_SIZE) lz = 0;

        int flat = (lx * CHUNK_SIZE + ly) * CHUNK_SIZE + lz;
        return (nb.data.data[flat] & 0xFF) != 0;
    }

    private void updateSeamsAround(int cx, int cy, int cz) {
        long k = key(cx, cy, cz);
        LoadedChunk lc = loaded.get(k);
        if (lc == null) return;

        // iterate 6 neighbors and update border masks on both sides if neighbor is loaded
        updateSeam(lc, +1, 0, 0); // +X
        updateSeam(lc, -1, 0, 0); // -X
        updateSeam(lc, 0, +1, 0); // +Y
        updateSeam(lc, 0, -1, 0); // -Y
        updateSeam(lc, 0, 0, +1); // +Z
        updateSeam(lc, 0, 0, -1); // -Z
    }

    private void updateSeam(LoadedChunk lc, int dx, int dy, int dz) {
        int ncx = lc.data.cx + dx;
        int ncy = lc.data.cy + dy;
        int ncz = lc.data.cz + dz;
        LoadedChunk nb = loaded.get(key(ncx, ncy, ncz));
        if (nb == null) return;

        // determine plane to update
        if (dx == 1) {
            // self x = 31, neighbor x = 0
            int sx = CHUNK_SIZE - 1, nx = 0;
            for (int y = 0; y < CHUNK_SIZE; y++) for (int z = 0; z < CHUNK_SIZE; z++) {
                int sFlat = (sx * CHUNK_SIZE + y) * CHUNK_SIZE + z;
                Terrain st = lc.terrainByIndex[sFlat];
                if (st != null) st.setFaceMask(computeMaskFor(lc, sx, y, z));

                int nFlat = (nx * CHUNK_SIZE + y) * CHUNK_SIZE + z;
                Terrain nt = nb.terrainByIndex[nFlat];
                if (nt != null) nt.setFaceMask(computeMaskFor(nb, nx, y, z));
            }
        } else if (dx == -1) {
            // self x = 0, neighbor x = 31
            int sx = 0, nx = CHUNK_SIZE - 1;
            for (int y = 0; y < CHUNK_SIZE; y++) for (int z = 0; z < CHUNK_SIZE; z++) {
                int sFlat = (sx * CHUNK_SIZE + y) * CHUNK_SIZE + z;
                Terrain st = lc.terrainByIndex[sFlat];
                if (st != null) st.setFaceMask(computeMaskFor(lc, sx, y, z));

                int nFlat = (nx * CHUNK_SIZE + y) * CHUNK_SIZE + z;
                Terrain nt = nb.terrainByIndex[nFlat];
                if (nt != null) nt.setFaceMask(computeMaskFor(nb, nx, y, z));
            }
        } else if (dy == 1) {
            int sy = CHUNK_SIZE - 1, ny = 0;
            for (int x = 0; x < CHUNK_SIZE; x++) for (int z = 0; z < CHUNK_SIZE; z++) {
                int sFlat = (x * CHUNK_SIZE + sy) * CHUNK_SIZE + z;
                Terrain st = lc.terrainByIndex[sFlat];
                if (st != null) st.setFaceMask(computeMaskFor(lc, x, sy, z));

                int nFlat = (x * CHUNK_SIZE + ny) * CHUNK_SIZE + z;
                Terrain nt = nb.terrainByIndex[nFlat];
                if (nt != null) nt.setFaceMask(computeMaskFor(nb, x, ny, z));
            }
        } else if (dy == -1) {
            int sy = 0, ny = CHUNK_SIZE - 1;
            for (int x = 0; x < CHUNK_SIZE; x++) for (int z = 0; z < CHUNK_SIZE; z++) {
                int sFlat = (x * CHUNK_SIZE + sy) * CHUNK_SIZE + z;
                Terrain st = lc.terrainByIndex[sFlat];
                if (st != null) st.setFaceMask(computeMaskFor(lc, x, sy, z));

                int nFlat = (x * CHUNK_SIZE + ny) * CHUNK_SIZE + z;
                Terrain nt = nb.terrainByIndex[nFlat];
                if (nt != null) nt.setFaceMask(computeMaskFor(nb, x, ny, z));
            }
        } else if (dz == 1) {
            int sz = CHUNK_SIZE - 1, nz = 0;
            for (int x = 0; x < CHUNK_SIZE; x++) for (int y = 0; y < CHUNK_SIZE; y++) {
                int sFlat = (x * CHUNK_SIZE + y) * CHUNK_SIZE + sz;
                Terrain st = lc.terrainByIndex[sFlat];
                if (st != null) st.setFaceMask(computeMaskFor(lc, x, y, sz));

                int nFlat = (x * CHUNK_SIZE + y) * CHUNK_SIZE + nz;
                Terrain nt = nb.terrainByIndex[nFlat];
                if (nt != null) nt.setFaceMask(computeMaskFor(nb, x, y, nz));
            }
        } else if (dz == -1) {
            int sz = 0, nz = CHUNK_SIZE - 1;
            for (int x = 0; x < CHUNK_SIZE; x++) for (int y = 0; y < CHUNK_SIZE; y++) {
                int sFlat = (x * CHUNK_SIZE + y) * CHUNK_SIZE + sz;
                Terrain st = lc.terrainByIndex[sFlat];
                if (st != null) st.setFaceMask(computeMaskFor(lc, x, y, sz));

                int nFlat = (x * CHUNK_SIZE + y) * CHUNK_SIZE + nz;
                Terrain nt = nb.terrainByIndex[nFlat];
                if (nt != null) nt.setFaceMask(computeMaskFor(nb, x, y, nz));
            }
        }
    }
}
