package cinnamon.registry;

import cinnamon.utils.Resource;

public enum TerrainModelRegistry {

    BOX("models/terrain/box/box.obj"),
    SPHERE("models/terrain/sphere/sphere.obj"),
    TEAPOT("models/terrain/teapot/teapot.obj"),
    GLTF_TEST("models/terrain/gltf_test/gltf_test.gltf");

    public final Resource resource;

    TerrainModelRegistry(String path) {
        this.resource = new Resource(path);
    }
}
