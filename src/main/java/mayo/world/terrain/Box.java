package mayo.world.terrain;

import mayo.registry.TerrainRegistry;

public class Box extends Terrain {
    @Override
    public TerrainRegistry getType() {
        return TerrainRegistry.BOX;
    }
}
