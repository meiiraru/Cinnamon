package cinnamon.render;

import cinnamon.render.framebuffer.Framebuffer;
import cinnamon.render.framebuffer.PBRDeferredFramebuffer;
import cinnamon.render.shader.Shader;
import cinnamon.render.shader.Shaders;
import cinnamon.render.texture.Texture;

import static org.lwjgl.opengl.GL11.*;

public class SSRRenderer {

    public static final Framebuffer
            ssrFramebuffer = new Framebuffer(Framebuffer.COLOR_BUFFER),
            ssrBlurFramebuffer = new Framebuffer(Framebuffer.COLOR_BUFFER);

    private static int out = 0;

    public static void render(PBRDeferredFramebuffer gBuffer, int prevFrame, Camera camera, int quality, boolean blur) {
        ssrFramebuffer.resizeTo(gBuffer);
        ssrFramebuffer.useClear();

        Shader s = Shaders.SSR.getShader().use();
        s.setTexture("previousTex", prevFrame, 0);
        s.setTexture("gNormal", gBuffer.getNormal(), 1);
        s.setTexture("gORM", gBuffer.getORM(), 2);
        s.setTexture("gDepth", gBuffer.getDepthBuffer(), 3);

        s.setFloat("nearPlane", Camera.NEAR_PLANE);
        s.setFloat("farPlane", Camera.FAR_PLANE);
        s.setup(camera);
        s.setupInverse(camera);

        float rayStep = 2f / (quality + 1f);
        s.setInt("maxSteps", (quality + 1) * 20);
        s.setFloat("rayStep", rayStep);

        glDisable(GL_BLEND);
        WorldRenderer.renderQuad();
        out = ssrFramebuffer.getColorBuffer();

        glEnable(GL_BLEND);
        Texture.unbindAll(4);

        if (blur) {
            Blur.boxBlur(ssrFramebuffer.getColorBuffer(), ssrFramebuffer.getWidth(), ssrFramebuffer.getHeight(), 2f, 2f, ssrBlurFramebuffer);
            out = ssrBlurFramebuffer.getColorBuffer();
        }
    }

    public static int getTexture() {
        return out;
    }
}
