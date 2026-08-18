package cinnamon.registry;

import cinnamon.utils.Resource;
import cinnamon.world.terrain.Barrier;
import cinnamon.world.terrain.Rose;
import cinnamon.world.terrain.Sphere;
import cinnamon.world.terrain.Teapot;
import cinnamon.world.terrain.Terrain;

import java.util.function.Supplier;

public enum TerrainRegistry {

    BOX(TerrainModelRegistry.BOX.resource),
    SPHERE(Sphere::new),
    SLAB(TerrainModelRegistry.SLAB.resource),
    TEAPOT(Teapot::new),
    ROSE(Rose::new),
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
