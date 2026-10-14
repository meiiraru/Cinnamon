package cinnamon.registry;

import cinnamon.render.shader.Shader;
import cinnamon.utils.Resource;

public enum CoreShadersRegistry {

    //vertex consumer
    MAIN,
    WORLD_MAIN,
    WORLD_MAIN_EMISSIVE,
    LINES,
    SCREEN_SPACE_UV,

    //gui
    BACKGROUND_COLOR,
    BACKGROUND_MENU,
    MODEL,

    //cubemap gen
    EQUIRECTANGULAR_TO_CUBEMAP,
    IRRADIANCE,
    PREFILTER,
    BRDF_LUT,

    //sky
    SKYBOX,
    CUBEMAP_SKYBOX,
    CLOUDS,

    //world
    GBUFFER_WORLD_PBR,
    DEFERRED_WORLD_PBR,
    GBUFFER_WORLD_PBR_WAVE,
    GBUFFER_TRANSPARENT,
    DEFERRED_TRANSPARENT,

    MAIN_PASS,
    MODEL_PASS,

    DECAL,
    FIRE,
    WATER,

    //lights
    LIGHT_GLARE,
    LENS_FLARE,
    LIGHT_PASS,
    VOLUMETRIC_LIGHT,

    DEPTH_DIR,
    MAIN_DEPTH_DIR,
    DEPTH,
    MAIN_DEPTH,
    POINT_DEPTH,
    POINT_MAIN_DEPTH,

    //post
    BRIGHT_PASS,
    BLOOM_COMPOSITE,
    SSAO,
    SSR,

    //misc
    OUTLINE,
    MODEL_UV;

    public final Resource resource;

    CoreShadersRegistry() {
        this.resource = new Resource("shaders/core/" + this.name().toLowerCase() + ".glsl");
    }

    public Shader getShader() {
        return Shader.of(resource);
    }
}
