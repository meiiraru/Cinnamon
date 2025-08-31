package cinnamon.render.shader;

import cinnamon.utils.Resource;

public enum Shaders {
    WORLD_FONT,
    WORLD_FONT_EMISSIVE,
    FONT,
    WORLD_MODEL_PBR,
    MODEL,
    WORLD_MAIN_EMISSIVE,
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
    BACKGROUND,
    BACKGROUND_NOISE,
    SCREEN_SPACE_UV,
    OUTLINE,
    MODEL_PASS,
    MODEL_UV,
    LIGHTING_PASS,
    BLIT_COLOR_DEPTH,
    MAIN_DEPTH,
    POINT_DEPTH,
    POINT_MAIN_DEPTH;

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
