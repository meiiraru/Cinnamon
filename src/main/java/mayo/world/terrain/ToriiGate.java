package mayo.world.terrain;

import mayo.model.ModelRegistry;
import mayo.world.World;

public class ToriiGate extends Terrain {

    public ToriiGate(World world) {
        super(ModelRegistry.Terrain.TORII_GATE.model, world);
    }
}
