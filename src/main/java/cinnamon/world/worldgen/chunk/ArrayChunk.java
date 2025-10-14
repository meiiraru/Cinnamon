package cinnamon.world.worldgen.chunk;

import cinnamon.render.Camera;
import cinnamon.render.MatrixStack;
import cinnamon.utils.AABB;
import cinnamon.world.terrain.Terrain;
import cinnamon.world.world.World;
import org.joml.Math;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class ArrayChunk extends Chunk {

    private final Terrain[][][] terrains = new Terrain[CHUNK_SIZE][CHUNK_SIZE][CHUNK_SIZE];

    public ArrayChunk(int x, int y, int z) {
        super(x, y, z);
    }

    @Override
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

    @Override
    public int render(Camera camera, MatrixStack matrices, float delta) {
        int i = 0;
        for (Terrain[][] tx : terrains) {
            for (Terrain[] ty : tx) {
                for (Terrain t : ty) {
                    if (t != null && t.shouldRender(camera)) {
                        t.render(camera, matrices, delta);
                        i++;
                    }
                }
            }
        }
        return i;
    }

    @Override
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

    @Override
    public Terrain getTerrainAtPos(float x, float y, float z) {
        if (x < 0 || y < 0 || z < 0 || x >= CHUNK_SIZE || y >= CHUNK_SIZE || z >= CHUNK_SIZE)
            return null;
        return terrains[(int) x][(int) y][(int) z];
    }

    @Override
    public void setTerrain(Terrain terrain, float x, float y, float z) {
        if (x < 0 || y < 0 || z < 0 || x >= CHUNK_SIZE || y >= CHUNK_SIZE || z >= CHUNK_SIZE)
            throw new IllegalArgumentException(String.format("Invalid position: %s, %s, %s", x, y, z));
        terrains[(int) x][(int) y][(int) z] = terrain;
        if (terrain != null)
            terrain.setPos(x + gridPos.x * CHUNK_SIZE, y + gridPos.y * CHUNK_SIZE, z + gridPos.z * CHUNK_SIZE);
    }

    @Override
    public Collection<Terrain> getTerrainInArea(AABB area) {
        Set<Terrain> set = new HashSet<>();

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
                        set.add(t);
                }
            }
        }

        return set;
    }
}
