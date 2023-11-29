package mayo.world.terrain;

import mayo.registry.TerrainRegistry;

public class Fence extends Terrain {
    @Override
    public TerrainRegistry getType() {
        return TerrainRegistry.FENCE;
    }
}
