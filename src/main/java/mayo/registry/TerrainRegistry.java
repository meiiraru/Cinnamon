package mayo.registry;

import com.esotericsoftware.kryo.Kryo;
import mayo.model.ModelManager;
import mayo.render.Model;
import mayo.utils.Resource;
import mayo.world.terrain.*;

import java.util.function.Supplier;

public enum TerrainRegistry implements Registry {
    BOX(Box.class, Box::new),
    SPHERE(Sphere.class, Sphere::new),
    TEAPOT(Teapot.class, Teapot::new);

    private static final String MODELS_PATH = "models/terrain/";

    public final Resource resource;
    public Model model;

    private final Class<? extends Terrain> clazz;
    private final Supplier<Terrain> factory;

    TerrainRegistry(Class<? extends Terrain> clazz, Supplier<Terrain> factory) {
        //fields
        this.clazz = clazz;
        this.factory = factory;

        //model
        String name = name().toLowerCase();
        this.resource = new Resource(MODELS_PATH + name + "/" + name + ".obj");
    }

    @Override
    public void register(Kryo kryo) {
        kryo.register(clazz);
    }

    public Supplier<Terrain> getFactory() {
        return factory;
    }

    private void loadModel() {
        this.model = ModelManager.load(this.resource);
    }

    public static void loadAllModels() {
        for (TerrainRegistry terrain : values())
            terrain.loadModel();
    }
}
