package cinnamon.render;

import cinnamon.render.framebuffer.Framebuffer;
import cinnamon.render.shader.PostProcess;
import cinnamon.render.shader.Shader;

import static cinnamon.render.WorldRenderer.renderQuad;

public class Blur {

    private static final Framebuffer blurBufferA = new Framebuffer(Framebuffer.HDR_COLOR_BUFFER);
    private static final Framebuffer blurBufferB = new Framebuffer(Framebuffer.HDR_COLOR_BUFFER);

    public static int blurTexture(int texture, int width, int height, float blurRadius) {
        int w = (int) (width / blurRadius);
        int h = (int) (height / blurRadius);

        blurBufferA.resize(w, h);
        blurBufferB.resize(w, h);

        Shader sh = PostProcess.GAUSSIAN_BLUR.getShader().use();
        sh.setVec2("texelSize", 1f / blurBufferA.getWidth(), 1f / blurBufferA.getHeight());

        int tex = texture;
        Framebuffer target = blurBufferA;
        target.adjustViewPort();

        //apply blur
        for (int i = 0; i < 10; i++) {
            boolean horizontal = i % 2 == 0;

            target.use();
            sh.setTexture("colorTex", tex, 0);
            sh.setVec2("dir", horizontal ? 1f : 0f, horizontal ? 0f : 1f);

            renderQuad();

            tex = target.getColorBuffer();
            target = target == blurBufferA ? blurBufferB : blurBufferA;
        }

        return tex;
    }
}
