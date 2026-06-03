package cinnamon.registry;

import cinnamon.utils.Resource;

public enum TerrainModelRegistry {

    BOX("models/terrain/box/box.obj"),
    SPHERE("models/terrain/sphere/sphere.obj"),
    TEAPOT("models/terrain/teapot/teapot.obj"),
    ROSE("models/terrain/rose/rose.obj"),
    GLTF_TEST("models/terrain/gltf_test/gltf_test.gltf"),

    CONVEYOR_BELT("models/terrain/conveyor_belt/model.obj"),
    BUTTON("models/terrain/button/button.obj");

    public final Resource resource;

    TerrainModelRegistry(String path) {
        this.resource = new Resource(path);
    }
}
