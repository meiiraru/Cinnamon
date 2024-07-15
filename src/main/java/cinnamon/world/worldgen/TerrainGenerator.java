package cinnamon.world.worldgen;

import cinnamon.registry.MaterialRegistry;
import cinnamon.registry.TerrainRegistry;
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
                    t.setMaterial((x == 0 && z == 0 && i == 0 && j == 0 ? CENTER_MATERIAL : PLAIN_MATERIAL).material);
                    chunk.setTerrain(t, i, 0, j);
                }
            }
        }

        return chunk;
    }
}
