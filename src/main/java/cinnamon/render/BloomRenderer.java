package cinnamon.render;

import cinnamon.render.framebuffer.Framebuffer;
import cinnamon.render.shader.PostProcess;
import cinnamon.render.shader.Shader;
import cinnamon.render.shader.Shaders;
import cinnamon.render.texture.Texture;

import static cinnamon.render.WorldRenderer.renderQuad;
import static org.lwjgl.opengl.GL11.*;

public class BloomRenderer {

    public static final Framebuffer brightPass = new Framebuffer(Framebuffer.HDR_COLOR_BUFFER);

    public static void applyBloom(Framebuffer targetBuffer, int emissiveTex, float threshold, float strength) {
        //get brightness
        brightPass.resizeTo(targetBuffer);
        brightPass.useClear();

        Shader s = Shaders.BRIGHT_PASS.getShader().use();
        s.setTexture("colorTex", targetBuffer.getColorBuffer(), 0);
        s.setTexture("emissiveTex", emissiveTex, 1);
        s.setFloat("threshold", threshold);
        renderQuad();

        //apply blur
        int blurTex = Blur.blurTexture(brightPass.getColorBuffer(), brightPass.getWidth(), brightPass.getHeight(), 2f);

        //composite back to the bright buffer with additive blending
        brightPass.useClear();
        brightPass.adjustViewPort();

        Shader sc = Shaders.BLOOM_COMPOSITE.getShader().use();
        sc.setTexture("sceneTex", targetBuffer.getColorBuffer(), 0);
        sc.setTexture("bloomTex", blurTex, 1);
        sc.setFloat("bloomStrength", strength);

        glBlendFunc(GL_ONE, GL_ONE);
        renderQuad();

        //and then blit to the target buffer
        targetBuffer.use();
        Shader blit = PostProcess.BLIT.getShader().use();
        blit.setTexture("colorTex", brightPass.getColorBuffer(), 0);

        glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
        renderQuad();

        //cleanup
        Texture.unbindAll(2);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    }
}
