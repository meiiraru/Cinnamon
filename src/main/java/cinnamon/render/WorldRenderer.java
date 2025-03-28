package cinnamon.render;

import cinnamon.model.SimpleGeometry;
import cinnamon.render.framebuffer.Blit;
import cinnamon.render.framebuffer.Framebuffer;
import cinnamon.render.framebuffer.PBRDeferredFramebuffer;
import cinnamon.render.shader.Shader;
import cinnamon.render.shader.Shaders;
import cinnamon.render.texture.Texture;
import cinnamon.world.world.WorldClient;

public class WorldRenderer {

    public static final PBRDeferredFramebuffer PBRFrameBuffer = new PBRDeferredFramebuffer(1, 1);
    public static final Framebuffer outlineFramebuffer = new Framebuffer(1, 1, Framebuffer.COLOR_BUFFER);

    private static boolean outlineRendering = false;
    private static Framebuffer previousFramebuffer;

    //public static final Material TERRAIN_MATERIAL = MaterialManager.load(new Resource("textures/terrain/terrain.pbr"), "terrain");

    public static void prepare(Camera camera) {
        previousFramebuffer = Framebuffer.activeFramebuffer;
        PBRFrameBuffer.useClear();
        PBRFrameBuffer.resizeTo(previousFramebuffer);
        previousFramebuffer.blit(PBRFrameBuffer.id(), false, false, true);
        Shader s = Shaders.GBUFFER_WORLD_PBR.getShader().use();
        s.setup(camera);
        s.setVec3("camPos", camera.getPosition());
    }

    public static void finish(WorldClient world) {
        previousFramebuffer.use();
        Shader s = Shaders.DEFERRED_WORLD_PBR.getShader().use();

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
        PBRFrameBuffer.blit(previousFramebuffer.id(), false, true, true);

        //cleanup textures
        Texture.unbindAll(tex);
        previousFramebuffer = null;
    }

    public static Shader prepareOutlineBuffer(Camera camera) {
        outlineRendering = true;
        previousFramebuffer = Framebuffer.activeFramebuffer;
        outlineFramebuffer.useClear();
        outlineFramebuffer.resizeTo(previousFramebuffer);
        Shader s = Shaders.MODEL_PASS.getShader().use();
        s.setup(camera);
        return s;
    }

    public static void finishOutlines() {
        //prepare outline
        previousFramebuffer.use();
        Shader outline = Shaders.OUTLINE.getShader().use();
        outline.setVec2("textelSize", 1f / WorldRenderer.outlineFramebuffer.getWidth(), 1f / WorldRenderer.outlineFramebuffer.getHeight());
        outline.setTexture("outlineTex", WorldRenderer.outlineFramebuffer.getColorBuffer(), 0);

        //render outline
        Blit.renderQuad();

        //cleanup
        Texture.unbindAll(1);
        outlineRendering = false;
        previousFramebuffer = null;
    }

    public static boolean isRenderingOutlines() {
        return outlineRendering;
    }
}
