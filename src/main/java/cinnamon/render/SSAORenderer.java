package cinnamon.render;

import cinnamon.render.framebuffer.Framebuffer;
import cinnamon.render.framebuffer.PBRDeferredFramebuffer;
import cinnamon.render.framebuffer.SSAOFramebuffer;
import cinnamon.render.shader.Shader;
import cinnamon.render.shader.Shaders;
import cinnamon.render.texture.Texture;

import static cinnamon.render.WorldRenderer.renderQuad;

public class SSAORenderer {

    public static final SSAOFramebuffer ssaoFramebuffer = new SSAOFramebuffer();
    public static final Framebuffer blurBuffer = new Framebuffer(Framebuffer.COLOR_BUFFER);
    private static int out = 0;

    public static void renderSSAO(PBRDeferredFramebuffer gBuffer, Camera camera, int quality, float radius) {
        //prepare framebuffer
        ssaoFramebuffer.resizeTo(gBuffer);
        ssaoFramebuffer.useClear();

        //set textures
        Shader s = Shaders.SSAO.getShader().use();
        s.setTexture("gNormal", gBuffer.getNormal(), 0);
        s.setTexture("gDepth", gBuffer.getDepthBuffer(), 1);
        s.setTexture("texKernel", SSAOFramebuffer.getKernelTexture(), 2);
        s.setTexture("texNoise", SSAOFramebuffer.getNoiseTexture(), 3);

        //set camera
        s.setup(camera);
        s.setupInverse(camera);
        s.setFloat("nearPlane", Camera.NEAR_PLANE);
        s.setFloat("farPlane", Camera.FAR_PLANE);

        //set kernel samples
        s.setInt("sampleCount", Math.max(quality, 1) * 16);
        s.setVec2("noiseScale", ssaoFramebuffer.getWidth() / 4f, ssaoFramebuffer.getHeight() / 4f);
        s.setFloat("radius", radius);

        //render quad
        renderQuad();
        out = ssaoFramebuffer.getSSAOTexture();

        //unbind textures
        Texture.unbindAll(4);
    }

    public static void blurSSAO() {
        out = Blur.boxBlur(ssaoFramebuffer.getSSAOTexture(), ssaoFramebuffer.getWidth(), ssaoFramebuffer.getHeight(), 2f, 1f, blurBuffer);
    }

    public static int getTexture() {
        return out;
    }
}
