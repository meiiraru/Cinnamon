package cinnamon.registry;

import cinnamon.utils.Resource;
import cinnamon.world.terrain.*;

import java.util.function.Supplier;

public enum TerrainRegistry {

    BOX(TerrainModelRegistry.BOX.resource),
    SPHERE(Sphere::new),
    SLAB(TerrainModelRegistry.SLAB.resource),
    TEAPOT(Teapot::new),
    ROSE(Rose::new),
    GLASS(Glass::new),
    BARRIER(Barrier::new),
    GLTF(TerrainModelRegistry.GLTF_TEST.resource),
    CONVEYOR_BELT(ConveyorBelt::new),
    TORII(TerrainModelRegistry.TORII_GATE.resource),
    TREE(Tree::new),
    TERMINAL(Terminal::new),
    BENCH(Bench::new),
    LAMP(Lamp::new),
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
