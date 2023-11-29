package mayo.world.terrain;

import mayo.model.ModelRegistry;

public class Sphere extends Terrain {
    @Override
    public ModelRegistry.Terrain getType() {
        return ModelRegistry.Terrain.SPHERE;
    }
}
