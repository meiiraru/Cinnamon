package mayo.world.terrain;

import mayo.model.ModelManager;
import mayo.render.Model;
import mayo.utils.Resource;
import mayo.world.World;

public class ToriiGate extends Terrain {

    private static final Model MODEL = ModelManager.load(new Resource("models/terrain/torii_gate/torii_gate.obj"));

    public ToriiGate(World world) {
        super(MODEL, world);
    }
}
