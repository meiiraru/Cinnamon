package cinnamon.registry;

import cinnamon.utils.Resource;

public enum SkyBoxRegistry {
    CLEAR,
    SPACE,
    CLOUDS,
    TEST,
    HDR_TEST(true),
    WHITE,
    DEBUG;

    public final boolean hdr;
    public final Resource resource;

    SkyBoxRegistry() {
        this(false);
    }

    SkyBoxRegistry(boolean hdr) {
        this.hdr = hdr;
        this.resource = new Resource("textures/environment/skybox/" + name().toLowerCase() + (hdr ? ".hdr" : ""));
    }
}
