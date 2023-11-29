package mayo.world.terrain;

import mayo.model.ModelRegistry;

public class Fence extends Terrain {
    @Override
    public ModelRegistry.Terrain getType() {
        return ModelRegistry.Terrain.FENCE;
    }
}
