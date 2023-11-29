package mayo.world.terrain;

import mayo.model.ModelRegistry;

public class ToriiGate extends Terrain {
    @Override
    public ModelRegistry.Terrain getType() {
        return ModelRegistry.Terrain.TORII_GATE;
    }
}
