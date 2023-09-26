package mayo.world.terrain;

import mayo.model.ModelManager;
import mayo.render.Model;
import mayo.utils.Resource;
import mayo.world.World;

public enum TerrainRegister {
    PILLAR,
    TEAPOT,
    TORII,
    GRASS,
    POLE;

    private final Model model;

    TerrainRegister() {
        String name = name().toLowerCase();
        this.model = ModelManager.load(new Resource("models/terrain/" + name + "/" + name + ".obj"));
    }

    public TerrainObject create(World world) {
        return new TerrainObject(model, world);
    }
}
