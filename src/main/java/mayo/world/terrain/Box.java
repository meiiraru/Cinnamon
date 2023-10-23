package mayo.world.terrain;

import mayo.model.ModelRegistry;
import mayo.world.World;

public class Box extends Terrain {

    public Box(World world) {
        super(ModelRegistry.Terrain.BOX.model, world);
    }
}
