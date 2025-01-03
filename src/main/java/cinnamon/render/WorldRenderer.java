package cinnamon.render;

import cinnamon.model.SimpleGeometry;
import cinnamon.render.framebuffer.Framebuffer;
import cinnamon.render.framebuffer.PBRDeferredFramebuffer;
import cinnamon.render.shader.Shader;
import cinnamon.render.shader.Shaders;
import cinnamon.render.texture.Texture;
import cinnamon.world.world.WorldClient;

public class WorldRenderer {

    private static final Shaders
            GEOMETRY_PASS = Shaders.GBUFFER_WORLD_PBR,
            LIGHTING_PASS = Shaders.DEFERRED_WORLD_PBR;

    public static final PBRDeferredFramebuffer PBRFrameBuffer = new PBRDeferredFramebuffer(1, 1);

    //public static final Material TERRAIN_MATERIAL = MaterialManager.load(new Resource("textures/terrain/terrain.pbr"), "terrain");

    public static void prepare(Camera camera) {
        PBRFrameBuffer.useClear();
        Framebuffer.DEFAULT_FRAMEBUFFER.blit(PBRFrameBuffer.id(), false, false, true);
        Shader s = GEOMETRY_PASS.getShader().use();
        s.setup(camera.getProjectionMatrix(), camera.getViewMatrix());
        s.setVec3("camPos", camera.getPos());
    }

    public static void finish(WorldClient world) {
        Framebuffer.DEFAULT_FRAMEBUFFER.use();
        Shader s = LIGHTING_PASS.getShader().use();

        //world uniforms
        world.applyWorldUniforms(s);
        //world.applyShadowUniforms(s);
        world.getSkyBox().pushToShader(s, Texture.MAX_TEXTURES - 1);

        //gbuffer textures
        s.setInt("gPosition", 0);
        s.setInt("gAlbedo", 1);
        s.setInt("gORM", 2);
        s.setInt("gNormal", 3);
        s.setInt("gEmissive", 4);
        int tex = PBRFrameBuffer.bindTextures();

        //render and blit to main framebuffer
        SimpleGeometry.QUAD.render();
        PBRFrameBuffer.blit(Framebuffer.DEFAULT_FRAMEBUFFER.id(), false, true, true);

        //cleanup textures
        Texture.unbindAll(tex);
    }

    public static void resize(int width, int height) {
        PBRFrameBuffer.resize(width, height);
    }
}
