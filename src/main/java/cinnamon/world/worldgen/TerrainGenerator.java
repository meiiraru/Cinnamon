package cinnamon.world.worldgen;

import cinnamon.registry.MaterialRegistry;
import cinnamon.registry.TerrainRegistry;
import cinnamon.world.world.World;
import cinnamon.world.terrain.Terrain;

public class TerrainGenerator {

    private static final MaterialRegistry
            PLAIN_MATERIAL = MaterialRegistry.GRASS,
            CENTER_MATERIAL = MaterialRegistry.COBBLESTONE;

    public static Chunk generatePlain(int x, int y, int z) {
        Chunk chunk = new Chunk(x, y, z);

        if (y == 0) {
            for (int i = 0; i < Chunk.CHUNK_SIZE; i++) {
                for (int j = 0; j < Chunk.CHUNK_SIZE; j++) {
                    Terrain t = TerrainRegistry.BOX.getFactory().get();
                    t.setMaterial(x == 0 && z == 0 && i == 0 && j == 0 ? CENTER_MATERIAL : PLAIN_MATERIAL);
                    chunk.setTerrain(t, i, 0, j);
                }
            }
        }

        return chunk;
    }

public static void generateMengerSponge(World world, int level, int xOffset, int yOffset, int zOffset) {
    int size = (int) Math.pow(3, level);
    int[] mod = new int[size];

    for (int i = 1; i <= level; i++) {
        int e1 = (int) Math.pow(3, i - 1);
        int e2 = e1 * 3;
        int e3 = e1 * 2;

        for (int j = 0; j < size; j++) {
            int t = j % e2;
            mod[j] = (e1 <= t) && (t < e3) ? 1 : 0;
        }

        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                for (int z = 0; z < size; z++) {
                    if (mod[x] + mod[y] + mod[z] > 1) {
                        world.setTerrain(null, x + xOffset, y + yOffset, z + zOffset);
                    } else if (i == 1) {
                        Terrain terr = TerrainRegistry.BOX.getFactory().get();
                        terr.setMaterial(MaterialRegistry.GOLD);
                        world.setTerrain(terr, x + xOffset, y + yOffset, z + zOffset);
                    }
                }
            }
        }
    }
}
}
