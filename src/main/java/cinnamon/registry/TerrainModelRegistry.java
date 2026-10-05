package cinnamon.registry;

import cinnamon.utils.Resource;

public enum TerrainModelRegistry {

    BOX("models/terrain/box/box.obj"),
    SPHERE("models/terrain/sphere/sphere.obj"),
    SLAB("models/terrain/slab/slab.obj"),
    TEAPOT("models/terrain/teapot/teapot.obj"),
    ROSE("models/terrain/rose/rose.obj"),
    GLASS("models/terrain/glass/glass.obj"),
    TERMINAL("models/terrain/terminal/terminal.obj"),
    BENCH("models/terrain/bench/bench.obj"),
    LAMP("models/terrain/lamp/lamp.obj"),

    CONVEYOR_BELT("models/terrain/conveyor_belt/model.obj"),
    BUTTON("models/terrain/button/button.obj"),
    TORII_GATE("models/terrain/torii/torii_gate.obj"),
    TREE("models/terrain/tree/deer_tree.obj"),
    TREE_HITBOX("models/terrain/tree/deer_tree_hitbox.obj"),

    GLTF_TEST("models/terrain/gltf_test/gltf_test.gltf");

    public final Resource resource;

    TerrainModelRegistry(String path) {
        this.resource = new Resource(path);
    }
}
