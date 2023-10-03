package mayo.world.terrain;

import mayo.model.ModelRegistry;
import mayo.world.World;

public class Fence extends Terrain {

    public Fence(World world) {
        super(ModelRegistry.Terrain.FENCE.model, world);
    }
}
