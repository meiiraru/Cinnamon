package cinnamon.render;

import cinnamon.model.SimpleGeometry;
import cinnamon.render.framebuffer.Blit;
import cinnamon.render.framebuffer.Framebuffer;
import cinnamon.render.framebuffer.PBRDeferredFramebuffer;
import cinnamon.render.shader.Shader;
import cinnamon.render.shader.Shaders;
import cinnamon.render.texture.Texture;
import cinnamon.utils.UIHelper;
import cinnamon.world.world.WorldClient;

import java.util.function.Consumer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.GL_STENCIL_INDEX;
import static org.lwjgl.opengl.GL43.GL_DEPTH_STENCIL_TEXTURE_MODE;

public class WorldRenderer {

    private static final Shaders
            GEOMETRY_PASS = Shaders.GBUFFER_WORLD_PBR,
            LIGHTING_PASS = Shaders.DEFERRED_WORLD_PBR,
            MODEL_PASS    = Shaders.MODEL_PASS,
            OUTLINE       = Shaders.OUTLINE;

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

    public static void prepareOutline(Camera camera) {
        //prepare model pass shader
        Shader model = MODEL_PASS.getShader().use();
        model.setup(
                camera.getProjectionMatrix(),
                camera.getViewMatrix()
        );

        //prepare outline shader
        Shader outline = OUTLINE.getShader().use();
        outline.setVec2("resolution", PBRFrameBuffer.getWidth(), PBRFrameBuffer.getHeight());
    }

    public static void renderOutline(Runnable render, Consumer<Shader> outlineConsumer) {
        //prepare model render
        UIHelper.prepareStencil();
        glDisable(GL_DEPTH_TEST);

        //render model
        MODEL_PASS.getShader().use();
        render.run();

        //finish model render
        UIHelper.lockStencil(false);
        glEnable(GL_DEPTH_TEST);
        UIHelper.disableStencil();

        //prepare outline
        Shader outline = OUTLINE.getShader().use();
        outline.setTexture("colorTex", Framebuffer.DEFAULT_FRAMEBUFFER.getColorBuffer(), 0);
        outline.setTexture("stencilTex", Framebuffer.DEFAULT_FRAMEBUFFER.getStencilBuffer(), 1);
        glTexParameteri(GL_TEXTURE_2D, GL_DEPTH_STENCIL_TEXTURE_MODE, GL_STENCIL_INDEX);
        outlineConsumer.accept(outline);

        //render outline
        Blit.renderQuad();
        Texture.unbindAll(2);
    }

    public static void resize(int width, int height) {
        PBRFrameBuffer.resize(width, height);
    }
}
