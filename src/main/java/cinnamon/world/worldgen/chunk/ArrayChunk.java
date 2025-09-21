package cinnamon.world.worldgen.chunk;

import cinnamon.render.Camera;
import cinnamon.render.MatrixStack;
import cinnamon.utils.AABB;
import cinnamon.render.model.CubeRenderer;
import cinnamon.world.terrain.Terrain;
import cinnamon.world.world.World;

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
        int rendered = 0;
        for (int x = 0; x < CHUNK_SIZE; x++) {
            for (int y = 0; y < CHUNK_SIZE; y++) {
                for (int z = 0; z < CHUNK_SIZE; z++) {
                    Terrain t = terrains[x][y][z];
                    if (t == null || !t.shouldRender(camera)) continue;

                    // face mask: bit per face: 0:+X,1:-X,2:+Y,3:-Y,4:+Z,5:-Z (1 means hide)
                    byte mask = 0;
                    if (isSolid(x + 1, y, z)) mask |= 1 << 0;
                    if (isSolid(x - 1, y, z)) mask |= 1 << 1;
                    if (isSolid(x, y + 1, z)) mask |= 1 << 2;
                    if (isSolid(x, y - 1, z)) mask |= 1 << 3;
                    if (isSolid(x, y, z + 1)) mask |= 1 << 4;
                    if (isSolid(x, y, z - 1)) mask |= 1 << 5;

                    matrices.pushMatrix();
                    matrices.translate(t.getPos().x + 0.5f - (int) (t.getPos().x) + (int) (t.getPos().x),
                                       t.getPos().y,
                                       t.getPos().z + 0.5f - (int) (t.getPos().z) + (int) (t.getPos().z));
                    CubeRenderer.renderFaces(matrices, t.getMaterial().material, mask);
                    matrices.popMatrix();
                    rendered++;
                }
            }
        }
        return rendered;
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

    private boolean isSolid(int x, int y, int z) {
        if (x >= 0 && x < CHUNK_SIZE && y >= 0 && y < CHUNK_SIZE && z >= 0 && z < CHUNK_SIZE)
            return terrains[x][y][z] != null;
        // neighbor chunk check would go here; fallback to non-solid if unknown
        return false;
    }
}
