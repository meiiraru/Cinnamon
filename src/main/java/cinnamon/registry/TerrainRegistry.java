package cinnamon.registry;

import cinnamon.utils.Resource;
import cinnamon.world.terrain.Teapot;
import cinnamon.world.terrain.Terrain;

import java.util.function.Supplier;

public enum TerrainRegistry {
    BOX,
    SPHERE,
    TEAPOT(Teapot::new),
    GLTF_TEST;

    private static final String MODELS_PATH = "models/terrain/";

    public final Resource resource;

    private final Supplier<Terrain> factory;

    TerrainRegistry() {
        this(null);
    }

    TerrainRegistry(Supplier<Terrain> factory) {
        //model
        String name = name().toLowerCase();
        this.resource = new Resource(MODELS_PATH + name + "/" + name + (name.contains("gltf") ? ".gltf" : ".obj"));

        //factory
        this.factory = factory != null ? factory : () -> new Terrain(this);
    }

    public Supplier<Terrain> getFactory() {
        return factory;
    }
}
