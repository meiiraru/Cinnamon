package mayo.render;

import mayo.model.SimpleGeometry;
import mayo.render.framebuffer.Framebuffer;
import mayo.render.framebuffer.PBRDeferredFramebuffer;
import mayo.render.shader.Shader;
import mayo.render.shader.Shaders;
import mayo.render.texture.Texture;

import java.util.function.Consumer;

public class WorldRenderer {

    private static final Shaders
            GEOMETRY_PASS = Shaders.GBUFFER_WORLD_PBR,
            LIGHTING_PASS = Shaders.DEFERRED_WORLD_PBR;

    private static final PBRDeferredFramebuffer PBRFrameBuffer = new PBRDeferredFramebuffer(1, 1);

    public static Shader prepareGeometry() {
        PBRFrameBuffer.useClear();
        return GEOMETRY_PASS.getShader().use();
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
