package mayo.world.terrain;

import mayo.model.ModelRegistry;

public class Pillar extends Terrain {
    @Override
    public ModelRegistry.Terrain getType() {
        return ModelRegistry.Terrain.PILLAR;
    }
}
