package mayo.world.terrain;

import mayo.model.ModelRegistry;

public class Box extends Terrain {
    @Override
    public ModelRegistry.Terrain getType() {
        return ModelRegistry.Terrain.BOX;
    }
}
