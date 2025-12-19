package cinnamon.render;

import cinnamon.render.framebuffer.PBRDeferredFramebuffer;
import cinnamon.render.framebuffer.SSAOFramebuffer;
import cinnamon.render.shader.Shader;
import cinnamon.render.shader.Shaders;
import cinnamon.render.texture.Texture;
import cinnamon.settings.Settings;
import cinnamon.utils.Maths;
import org.joml.Math;
import org.joml.Random;
import org.joml.Vector3f;

import static cinnamon.render.WorldRenderer.renderQuad;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.GL_RGB16F;

public class SSAORenderer {

    private static final Vector3f[] sampleKernel = new Vector3f[64];
    private static final int noiseTexture;
    public static final SSAOFramebuffer
            ssaoFramebuffer = new SSAOFramebuffer(),
            ssaoBlurFramebuffer = new SSAOFramebuffer();

    private static int out = ssaoFramebuffer.getTexture();

    static {
        Random rand = new Random(42L);

        //generate sample kernel
        for (int i = 0; i < sampleKernel.length; i++) {
            sampleKernel[i] = new Vector3f(
                    rand.nextFloat() * 2f - 1f,
                    rand.nextFloat() * 2f - 1f,
                    rand.nextFloat()
            ).normalize().mul(rand.nextFloat());

            float scale = (float) i / sampleKernel.length;
            scale = Math.lerp(0.1f, 1f, scale * scale);
            sampleKernel[i].mul(scale);
        }

        //generate noise vectors
        float[] noiseVectors = new float[16 * 3];
        for (int i = 0; i < noiseVectors.length; i++)
            noiseVectors[i] = i % 3 == 2 ? 0f : (rand.nextFloat() * 2f - 1f);

        //generate noise texture
        noiseTexture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, noiseTexture);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB16F, 4, 4, 0, GL_RGB, GL_FLOAT, noiseVectors);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);

        glBindTexture(GL_TEXTURE_2D, 0);
    }

    public static void renderSSAO(PBRDeferredFramebuffer gBuffer, Camera camera, float radius) {
        //prepare framebuffer
        ssaoFramebuffer.resizeTo(gBuffer);
        ssaoFramebuffer.useClear();

        //set textures
        Shader s = Shaders.SSAO.getShader().use();
        s.setTexture("gNormal", gBuffer.getNormal(), 0);
        s.setTexture("gDepth", gBuffer.getDepthBuffer(), 1);
        s.setTexture("texNoise", noiseTexture, 2);

        //set camera
        s.setup(camera);
        s.setupInverse(camera);

        //set kernel samples
        s.setInt("sampleCount", Maths.clamp(Settings.ssaoLevel.get(), 1, 4) * 16);
        s.setVec3Array("samples", sampleKernel);
        s.setVec2("noiseScale", ssaoFramebuffer.getWidth() / 4f, ssaoFramebuffer.getHeight() / 4f);
        s.setFloat("radius", radius);

        //render quad
        renderQuad();
        out = ssaoFramebuffer.getTexture();

        //unbind textures
        Texture.unbindAll(3);
    }

    public static void blurSSAO() {
        //prepare framebuffer
        ssaoBlurFramebuffer.resizeTo(ssaoFramebuffer);
        ssaoBlurFramebuffer.useClear();

        //setup blur shader
        Shader sh = Shaders.SSAO_BLUR.getShader().use();
        sh.setTexture("ssaoTex", ssaoFramebuffer.getTexture(), 0);
        sh.setVec2("texelSize", 1f / ssaoBlurFramebuffer.getWidth(), 1f / ssaoBlurFramebuffer.getHeight());

        //render quad
        renderQuad();
        out = ssaoBlurFramebuffer.getTexture();

        //unbind textures
        Texture.unbindAll(1);
    }

    public static int getSSAOTexture() {
        return out;
    }

    public static int getNoiseTexture() {
        return noiseTexture;
    }
}
