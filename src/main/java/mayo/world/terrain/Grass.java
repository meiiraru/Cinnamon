package mayo.world.terrain;

import mayo.registry.TerrainRegistry;

public class Grass extends Terrain {
    @Override
    public TerrainRegistry getType() {
        return TerrainRegistry.GRASS;
    }
}
