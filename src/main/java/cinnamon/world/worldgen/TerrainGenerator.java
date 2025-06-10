package cinnamon.world.worldgen;

import cinnamon.registry.MaterialRegistry;
import cinnamon.registry.TerrainRegistry;
import cinnamon.world.terrain.Terrain;
import cinnamon.world.world.World;

public class TerrainGenerator {

    public static void fill(World world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, MaterialRegistry material) {
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Terrain terrain = TerrainRegistry.BOX.getFactory().get();
                    terrain.setMaterial(material);
                    world.setTerrain(terrain, x, y, z);
                }
            }
        }
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
                            world.setTerrain(null, x + xOffset + 0.5f, y + yOffset + 0.5f, z + zOffset + 0.5f);
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
