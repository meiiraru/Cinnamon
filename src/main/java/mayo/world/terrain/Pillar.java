package mayo.world.terrain;

import mayo.model.ModelRegistry;
import mayo.world.World;

public class Pillar extends Terrain {

    public Pillar(World world) {
        super(ModelRegistry.Terrain.PILLAR.model, world);
    }
}
