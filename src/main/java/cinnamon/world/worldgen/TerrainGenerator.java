package cinnamon.world.worldgen;

import cinnamon.math.Maths;
import cinnamon.model.material.Material;
import cinnamon.registry.TerrainRegistry;
import cinnamon.world.terrain.Terrain;
import cinnamon.world.world.World;

public class TerrainGenerator {

    public static void fill(World world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, Material material) {
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Terrain terrain = TerrainRegistry.BOX.getFactory().get();
                    terrain.setMaterial(material);
                    terrain.setPos(x, y, z);
                    world.addTerrain(terrain);
                }
            }
        }
    }

    public static void generateMengerSponge(World world, int level, int xOffset, int yOffset, int zOffset, Material material) {
        int size = Maths.pow(3, level);
        boolean[][][] filled = new boolean[size][size][size];
        int[] mod = new int[size];

        for (int i = 1; i <= level; i++) {
            int e1 = Maths.pow(3, i - 1);
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
                            filled[x][y][z] = false;
                        } else if (i == 1) {
                            filled[x][y][z] = true;
                        }
                    }
                }
            }
        }

        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                for (int z = 0; z < size; z++) {
                    if (!filled[x][y][z])
                        continue;

                    Terrain terr = TerrainRegistry.BOX.getFactory().get();
                    terr.setMaterial(material);
                    terr.setPos(x + xOffset, y + yOffset, z + zOffset);
                    world.addTerrain(terr);
                }
            }
        }
    }
}
