package cinnamon.registry;

import com.esotericsoftware.kryo.Kryo;
import cinnamon.model.ModelManager;
import cinnamon.render.Model;
import cinnamon.utils.Resource;
import cinnamon.world.terrain.*;

import java.util.function.Supplier;

public enum TerrainRegistry implements Registry {
    BOX,
    SPHERE,
    TEAPOT(Teapot::new);

    private static final String MODELS_PATH = "models/terrain/";

    public final Resource resource;
    public Model model;

    private final Supplier<Terrain> factory;

    TerrainRegistry() {
        this(null);
    }

    TerrainRegistry(Supplier<Terrain> factory) {
        //model
        String name = name().toLowerCase();
        this.resource = new Resource(MODELS_PATH + name + "/" + name + ".obj");

        //factory
        this.factory = factory != null ? factory : () -> new Terrain(this);
    }

    @Override
    public void register(Kryo kryo) {
        //kryo.register(clazz);
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
