package cinnamon.render.shader;

import cinnamon.utils.Resource;

public enum Shaders {
    GUI,
    WORLD_FONT,
    FONT,
    WORLD_MODEL,
    WORLD_MODEL_PBR,
    MODEL,
    WORLD_MAIN,
    MAIN,
    LINES,
    DEPTH,
    DEPTH_BLIT,
    SKYBOX,
    IRRADIANCE,
    PREFILTER,
    BRDF_LUT,
    EQUIRECTANGULAR_TO_CUBEMAP,
    DEFERRED_WORLD_PBR,
    GBUFFER_WORLD_PBR,
    MAIN_MENU,
    SCREEN_SPACE_UV,
    OUTLINE;

    private final Resource resource;
    private Shader shader;

    Shaders() {
        this.resource = new Resource("shaders/core/" + this.name().toLowerCase() + ".glsl");
    }

    private void loadShader() {
        this.shader = new Shader(this.resource);
    }

    public Shader getShader() {
        return shader;
    }

    public static void freeAll() {
        for (Shaders shader : values())
            shader.getShader().free();
    }

    public static void loadAll() {
        for (Shaders shader : values())
            shader.loadShader();
    }
}
