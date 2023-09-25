package mayo.world.terrain;

import mayo.model.ModelManager;
import mayo.render.Model;
import mayo.utils.Resource;
import mayo.world.World;

public class GrassSquare extends TerrainObject {

    private static final Model MODEL = ModelManager.load(new Resource("models/terrain/grass/grass.obj"));

    public GrassSquare(World world) {
        super(MODEL, world);
    }
}
