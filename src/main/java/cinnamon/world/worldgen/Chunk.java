package cinnamon.world.worldgen;

import cinnamon.render.Camera;
import cinnamon.render.MatrixStack;
import cinnamon.utils.AABB;
import cinnamon.world.World;
import cinnamon.world.terrain.Terrain;
import org.joml.Vector3i;

import java.util.ArrayList;
import java.util.List;

public class Chunk {
    public static final int CHUNK_SIZE = 32;

    // -- temp -- //

    //todo - definitely not static, should be biome dependent
    public static final float fogDensity = 0.5f;

    public static final int fogColor = 0xC1E7FF;
    public static final int ambientLight = 0xFFFFFF;

    public static float getFogStart(World world) {
        return CHUNK_SIZE * (world.renderDistance - 2);
    }
    public static float getFogEnd(World world) {
        return CHUNK_SIZE * (world.renderDistance - 2) + (CHUNK_SIZE / fogDensity);
    }

    private final Terrain[][][] terrains = new Terrain[CHUNK_SIZE][CHUNK_SIZE][CHUNK_SIZE];

    private final Vector3i gridPos;
    private final AABB aabb;

    public Chunk(Vector3i pos) {
        this(pos.x, pos.y, pos.z);
    }

    public Chunk(int x, int y, int z) {
        this.gridPos = new Vector3i(x, y, z);
        int ax = x * CHUNK_SIZE;
        int ay = y * CHUNK_SIZE;
        int az = z * CHUNK_SIZE;
        this.aabb = new AABB(ax, ay, az, ax + CHUNK_SIZE, ay + CHUNK_SIZE, az + CHUNK_SIZE);
    }

    // -- end temp -- //

    public void tick() {
        for (Terrain[][] tx : terrains) {
            for (Terrain[] ty : tx) {
                for (Terrain t : ty) {
                    if (t != null)
                        t.tick();
                }
            }
        }
    }

    public int render(Camera camera, MatrixStack matrices, float delta) {
        int i = 0;
        for (Terrain[][] tx : terrains) {
            for (Terrain[] ty : tx) {
                for (Terrain t : ty) {
                    if (t != null && t.shouldRender(camera)) {
                        t.render(matrices, delta);
                        i++;
                    }
                }
            }
        }
        return i;
    }

    public void onAdded(World world) {
        for (Terrain[][] tx : terrains) {
            for (Terrain[] ty : tx) {
                for (Terrain t : ty) {
                    if (t != null)
                        t.onAdded(world);
                }
            }
        }
    }

    public boolean shouldRender(Camera camera) {
        return camera.isInsideFrustum(getAABB());
    }

    public Terrain getTerrainAtPos(Vector3i pos) {
        return getTerrainAtPos(pos.x, pos.y, pos.z);
    }

    public Terrain getTerrainAtPos(int x, int y, int z) {
        if (x < 0 || y < 0 || z < 0 || x >= CHUNK_SIZE || y >= CHUNK_SIZE || z >= CHUNK_SIZE)
            return null;
        return terrains[x][y][z];
    }

    public void setTerrain(Terrain terrain, Vector3i pos) {
        setTerrain(terrain, pos.x, pos.y, pos.z);
    }

    public void setTerrain(Terrain terrain, int x, int y, int z) {
        if (x < 0 || y < 0 || z < 0 || x >= CHUNK_SIZE || y >= CHUNK_SIZE || z >= CHUNK_SIZE)
            throw new IllegalArgumentException(String.format("Invalid position: %s, %s, %s", x, y, z));
        terrains[x][y][z] = terrain;
        if (terrain != null)
            terrain.setPos(x + gridPos.x * CHUNK_SIZE, y + gridPos.y * CHUNK_SIZE, z + gridPos.z * CHUNK_SIZE);
    }

    public List<Terrain> getTerrainInArea(AABB area) {
        List<Terrain> list = new ArrayList<>();

        int minX = Math.max(0, (int) Math.floor(area.getMin().x));
        int minY = Math.max(0, (int) Math.floor(area.getMin().y));
        int minZ = Math.max(0, (int) Math.floor(area.getMin().z));
        int maxX = Math.min(CHUNK_SIZE, (int) Math.ceil(area.getMax().x));
        int maxY = Math.min(CHUNK_SIZE, (int) Math.ceil(area.getMax().y));
        int maxZ = Math.min(CHUNK_SIZE, (int) Math.ceil(area.getMax().z));

        for (int x = minX; x < maxX; x++) {
            for (int y = minY; y < maxY; y++) {
                for (int z = minZ; z < maxZ; z++) {
                    Terrain t = getTerrainAtPos(x, y, z);
                    if (t != null)
                        list.add(t);
                }
            }
        }

        return list;
    }

    public AABB getAABB() {
        return aabb;
    }

    public Vector3i getGridPos() {
        return gridPos;
    }
}
