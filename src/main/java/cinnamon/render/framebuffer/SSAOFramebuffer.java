package cinnamon.render.framebuffer;

import org.joml.Math;
import org.joml.Random;
import org.joml.Vector3f;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL30.*;

public class SSAOFramebuffer extends Framebuffer {

    private static final int kernelTexture, noiseTexture;

    static {
        Random rand = new Random(42L);

        //generate sample kernel
        float[] sampleKernel = new float[64 * 3];
        for (int i = 0; i < 64; i++) {
            Vector3f sample = new Vector3f(
                    rand.nextFloat() * 2f - 1f,
                    rand.nextFloat() * 2f - 1f,
                    rand.nextFloat()
            ).normalize(rand.nextFloat());

            float scale = i / 64f;
            scale = Math.lerp(0.1f, 1f, scale * scale);
            sample.mul(scale);

            sampleKernel[i * 3]     = sample.x;
            sampleKernel[i * 3 + 1] = sample.y;
            sampleKernel[i * 3 + 2] = sample.z;
        }

        //generate kernel texture
        kernelTexture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, kernelTexture);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB16F, 64, 1, 0, GL_RGB, GL_FLOAT, sampleKernel);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

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

    private int texture;

    public SSAOFramebuffer() {
        super(0, GL_COLOR_BUFFER_BIT);
    }

    @Override
    protected void genBuffers() {
        use();

        texture = genTexture(GL_RED, getWidth(), getHeight(), GL_RED, GL_FLOAT, GL_LINEAR, GL_CLAMP_TO_EDGE, GL_COLOR_ATTACHMENT0);
        glBindTexture(GL_TEXTURE_2D, 0);

        checkForErrors();
        DEFAULT_FRAMEBUFFER.use();
    }

    @Override
    protected void freeTextures() {
        super.freeTextures();
        glDeleteTextures(texture);
    }

    public int getSSAOTexture() {
        return texture;
    }

    public static int getKernelTexture() {
        return kernelTexture;
    }

    public static int getNoiseTexture() {
        return noiseTexture;
    }
}
