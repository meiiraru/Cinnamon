package mayo.world.terrain;

import mayo.registry.TerrainRegistry;

public class Tree extends Terrain {
    @Override
    public TerrainRegistry getType() {
        return TerrainRegistry.TREE;
    }
}
