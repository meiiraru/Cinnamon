package cinnamon.registry;

import cinnamon.utils.Resource;
import cinnamon.world.terrain.Barrier;
import cinnamon.world.terrain.Teapot;
import cinnamon.world.terrain.Terrain;

import java.util.function.Supplier;

public enum TerrainRegistry {
    BOX("models/terrain/box/box.obj"),
    SPHERE("models/terrain/sphere/sphere.obj"),
    TEAPOT("models/terrain/teapot/teapot.obj", Teapot::new),
    GLTF_TEST("models/terrain/gltf_test/gltf_test.gltf"),
    BARRIER(null, Barrier::new);

    public final Resource resource;
    private final Supplier<Terrain> factory;

    TerrainRegistry(String path) {
        this(path, null);
    }

    TerrainRegistry(String path, Supplier<Terrain> factory) {
        this.resource = path == null ? null : new Resource(path);
        this.factory = factory != null ? factory : () -> new Terrain(this);
    }

    public Supplier<Terrain> getFactory() {
        return factory;
    }
}
