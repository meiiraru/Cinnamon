package mayo.world.terrain;

import mayo.model.ModelRegistry;

public class Tree extends Terrain {
    @Override
    public ModelRegistry.Terrain getType() {
        return ModelRegistry.Terrain.TREE;
    }
}
