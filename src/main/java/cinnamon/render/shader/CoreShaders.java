package cinnamon.render.shader;

import cinnamon.utils.Resource;

public enum CoreShaders {
    MODEL,
    WORLD_MAIN_EMISSIVE,
    WORLD_MAIN,
    MAIN,
    LINES,
    DEPTH,
    DEPTH_DIR,
    SKYBOX,
    IRRADIANCE,
    PREFILTER,
    BRDF_LUT,
    EQUIRECTANGULAR_TO_CUBEMAP,
    DEFERRED_WORLD_PBR,
    GBUFFER_WORLD_PBR,
    BACKGROUND_COLOR,
    BACKGROUND_MENU,
    SCREEN_SPACE_UV,
    OUTLINE,
    MODEL_PASS,
    MAIN_PASS,
    MODEL_UV,
    LIGHT_PASS,
    MAIN_DEPTH,
    MAIN_DEPTH_DIR,
    POINT_DEPTH,
    POINT_MAIN_DEPTH,
    BRIGHT_PASS,
    BLOOM_COMPOSITE,
    LENS_FLARE,
    LIGHT_GLARE,
    VOLUMETRIC_LIGHT,
    SSAO,
    SSR,
    WATER,
    FIRE,
    DECAL,
    CUBEMAP_SKYBOX,
    CLOUDS,
    GBUFFER_TRANSPARENT,
    DEFERRED_TRANSPARENT,
    GBUFFER_WORLD_PBR_WAVE;

    private final Resource resource;
    private Shader shader;

    CoreShaders() {
        this.resource = new Resource("shaders/core/" + this.name().toLowerCase() + ".glsl");
    }

    private void loadShader() {
        this.shader = Shader.of(this.resource);
    }

    public Shader getShader() {
        return shader;
    }

    public static void freeAll() {
        for (CoreShaders shader : values())
            shader.getShader().free();
    }

    public static void loadAll() {
        for (CoreShaders shader : values())
            shader.loadShader();
        CoreShaders.MAIN.getShader().use();
    }
}
