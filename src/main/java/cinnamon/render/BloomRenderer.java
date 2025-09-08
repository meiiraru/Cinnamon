package cinnamon.render;

import cinnamon.render.framebuffer.Framebuffer;
import cinnamon.render.shader.PostProcess;
import cinnamon.render.shader.Shader;
import cinnamon.render.shader.Shaders;
import cinnamon.render.texture.Texture;

import static cinnamon.render.WorldRenderer.renderQuad;

public class BloomRenderer {

    public static final Framebuffer brightPass = new Framebuffer(Framebuffer.HDR_COLOR_BUFFER);
    private static final Framebuffer blurBufferA = new Framebuffer(Framebuffer.HDR_COLOR_BUFFER);
    private static final Framebuffer blurBufferB = new Framebuffer(Framebuffer.HDR_COLOR_BUFFER);

    public static void applyBloom(Framebuffer targetBuffer, int emissiveTex, float threshold, float strength) {
        //get brightness
        brightPass.resizeTo(targetBuffer);
        brightPass.useClear();

        Shader s = Shaders.BRIGHT_PASS.getShader().use();
        s.setTexture("colorTex", targetBuffer.getColorBuffer(), 0);
        s.setTexture("gEmissiveTex", emissiveTex, 1);
        s.setFloat("threshold", threshold);
        renderQuad();

        blurBufferA.resizeTo(brightPass, 0.5f);
        blurBufferA.useClear();
        blurBufferB.resizeTo(brightPass, 0.5f);
        blurBufferA.useClear();

        Shader sh = PostProcess.GAUSSIAN_BLUR.getShader().use();
        sh.setVec2("texelSize", 1f / blurBufferA.getWidth(), 1f / blurBufferA.getHeight());

        Framebuffer source = brightPass;
        Framebuffer target = blurBufferA;
        target.adjustViewPort();

        //apply gaussian blur
        for (int i = 0; i < 10; i++) {
            boolean horizontal = i % 2 == 0;

            target.use();
            sh.setTexture("colorTex", source.getColorBuffer(), 0);
            sh.setVec2("dir", horizontal ? 1f : 0f, horizontal ? 0f : 1f);

            renderQuad();

            source = target;
            target = target == blurBufferA ? blurBufferB : blurBufferA;
        }

        //composite back to the bright buffer
        brightPass.useClear();
        brightPass.adjustViewPort();

        Shader sc = Shaders.BLOOM_COMPOSITE.getShader().use();
        sc.setTexture("sceneTex", targetBuffer.getColorBuffer(), 0);
        sc.setTexture("bloomTex", source.getColorBuffer(), 1);
        sc.setFloat("bloomStrength", strength);
        renderQuad();

        Shader blit = PostProcess.BLIT.getShader().use();
        blit.setTexture("colorTex", brightPass.getColorBuffer(), 0);

        targetBuffer.use();
        renderQuad();
        Texture.unbindAll(2);
    }
}
