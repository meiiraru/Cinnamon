package mayo.world.terrain;

import mayo.model.ModelManager;
import mayo.render.Model;
import mayo.utils.Resource;
import mayo.world.World;

public class Teapot extends TerrainObject {

    private static final Model MODEL = ModelManager.load(new Resource("models/terrain/teapot/teapot.obj"));

    public Teapot(World world) {
        super(MODEL, world);
    }
}
