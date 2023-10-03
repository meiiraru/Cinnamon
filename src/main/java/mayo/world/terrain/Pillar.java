package mayo.world.terrain;

import mayo.model.ModelManager;
import mayo.model.ModelRegistry;
import mayo.render.Model;
import mayo.utils.Resource;
import mayo.world.World;

public class Pillar extends Terrain {

    public Pillar(World world) {
        super(ModelRegistry.Terrain.PILLAR.model, world);
    }
}
