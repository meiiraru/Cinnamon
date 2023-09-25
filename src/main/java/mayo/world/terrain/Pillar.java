package mayo.world.terrain;

import mayo.model.ModelManager;
import mayo.render.Model;
import mayo.utils.Resource;
import mayo.world.World;

public class Pillar extends TerrainObject {

    private static final Model MODEL = ModelManager.load(new Resource("models/terrain/pillar/pillar.obj"));

    public Pillar(World world) {
        super(MODEL, world);
    }
}
