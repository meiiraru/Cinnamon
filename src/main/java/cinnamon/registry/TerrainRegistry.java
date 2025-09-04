package cinnamon.registry;

import cinnamon.utils.Resource;
import cinnamon.world.terrain.Barrier;
import cinnamon.world.terrain.Teapot;
import cinnamon.world.terrain.Terrain;

import java.util.function.Supplier;

public enum TerrainRegistry {

    BOX(TerrainModelRegistry.BOX.resource),
    SPHERE(TerrainModelRegistry.SPHERE.resource),
    TEAPOT(Teapot::new),
    BARRIER(Barrier::new),
    GLTF(TerrainModelRegistry.GLTF_TEST.resource),
    CUSTOM((Resource) null);

    private final Supplier<Terrain> factory;

    TerrainRegistry(Resource model) {
        this.factory = () -> new Terrain(model, this);
    }

    TerrainRegistry(Supplier<Terrain> factory) {
        this.factory = factory;
    }

    public Supplier<Terrain> getFactory() {
        return factory;
    }
}
