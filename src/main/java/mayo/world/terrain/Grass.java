package mayo.world.terrain;

import mayo.model.ModelManager;
import mayo.render.Model;
import mayo.utils.Resource;
import mayo.world.World;

public class Grass extends Terrain {

    private static final Model MODEL = ModelManager.load(new Resource("models/terrain/grass/grass.obj"));

    public Grass(World world) {
        super(MODEL, world);
    }
}
