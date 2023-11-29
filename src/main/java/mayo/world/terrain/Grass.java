package mayo.world.terrain;

import mayo.model.ModelRegistry;

public class Grass extends Terrain {
    @Override
    public ModelRegistry.Terrain getType() {
        return ModelRegistry.Terrain.GRASS;
    }
}
