package cinnamon.render;

import cinnamon.model.SimpleGeometry;
import cinnamon.render.framebuffer.Framebuffer;
import cinnamon.render.framebuffer.PBRDeferredFramebuffer;
import cinnamon.render.shader.Shader;
import cinnamon.render.shader.Shaders;
import cinnamon.render.texture.Texture;

import java.util.function.Consumer;

public class WorldRenderer {

    private static final Shaders
            GEOMETRY_PASS = Shaders.GBUFFER_WORLD_PBR,
            LIGHTING_PASS = Shaders.DEFERRED_WORLD_PBR;

    public static final PBRDeferredFramebuffer PBRFrameBuffer = new PBRDeferredFramebuffer(1, 1);

    //public static final Material TERRAIN_MATERIAL = MaterialManager.load(new Resource("textures/terrain/terrain.pbr"), "terrain");

    public static void prepareGeometry(Camera camera) {
        PBRFrameBuffer.useClear();
        Shader s = GEOMETRY_PASS.getShader().use();
        s.setup(camera.getPerspectiveMatrix(), camera.getViewMatrix());
        s.setVec3("camPos", camera.getPos());
    }

    public static void render(Consumer<Shader> shaderConsumer) {
        Framebuffer.DEFAULT_FRAMEBUFFER.use();
        Shader s = LIGHTING_PASS.getShader().use();
        shaderConsumer.accept(s);
        s.setInt("gPosition", 0);
        s.setInt("gAlbedo", 1);
        s.setInt("gRMAo", 2);
        s.setInt("gNormal", 3);
        s.setInt("gEmissive", 4);
        int tex = PBRFrameBuffer.bindTextures();
        SimpleGeometry.QUAD.render();
        PBRFrameBuffer.blit(Framebuffer.DEFAULT_FRAMEBUFFER.id(), false, true);
        Texture.unbindAll(tex);
    }

    public static void resize(int width, int height) {
        PBRFrameBuffer.resize(width, height);
    }
}
