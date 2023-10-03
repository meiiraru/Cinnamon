package mayo.world.terrain;

import mayo.model.ModelRegistry;
import mayo.world.World;

public class Grass extends Terrain {

    public Grass(World world) {
        super(ModelRegistry.Terrain.GRASS.model, world);
    }
}
