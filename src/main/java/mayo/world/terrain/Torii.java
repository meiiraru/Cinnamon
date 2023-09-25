package mayo.world.terrain;

import mayo.model.ModelManager;
import mayo.render.Model;
import mayo.utils.Resource;
import mayo.world.World;

public class Torii extends TerrainObject {

    private static final Model MODEL = ModelManager.load(new Resource("models/terrain/torii/torii.obj"));

    public Torii(World world) {
        super(MODEL, world);
    }
}
