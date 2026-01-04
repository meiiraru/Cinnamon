package cinnamon.render;

import cinnamon.render.framebuffer.Framebuffer;
import cinnamon.render.shader.PostProcess;
import cinnamon.render.shader.Shader;
import cinnamon.render.texture.Texture;

import static cinnamon.render.WorldRenderer.renderQuad;

public class Blur {

    public static int blurTexture(int texture, int width, int height, float blurRadius, Framebuffer pingPongA, Framebuffer pingPongB) {
        int w = (int) (width / blurRadius);
        int h = (int) (height / blurRadius);

        pingPongA.resize(w, h);
        pingPongB.resize(w, h);
        pingPongA.useClear();
        pingPongB.useClear();

        Shader sh = PostProcess.GAUSSIAN_BLUR.getShader().use();
        sh.setVec2("texelSize", 1f / w, 1f / h);

        int tex = texture;
        Framebuffer target = pingPongA;
        target.adjustViewPort();

        //apply blur
        for (int i = 0; i < 10; i++) {
            boolean horizontal = i % 2 == 0;

            target.use();
            sh.setTexture("colorTex", tex, 0);
            sh.setVec2("dir", horizontal ? 1f : 0f, horizontal ? 0f : 1f);

            renderQuad();

            tex = target.getColorBuffer();
            target = target == pingPongA ? pingPongB : pingPongA;
        }

        Texture.unbindAll(1);
        return tex;
    }

    public static int boxBlur(int texture, int width, int height, float blurRadius, float scale, Framebuffer blurBuffer) {
        blurBuffer.resize(width, height);
        blurBuffer.useClear();
        blurBuffer.adjustViewPort();

        Shader sh = PostProcess.BOX_BLUR.getShader().use();
        sh.setTexture("colorTex", texture, 0);
        sh.setVec2("texelSize", scale / width, scale / height);
        sh.setFloat("radius", blurRadius);

        renderQuad();

        Texture.unbindAll(1);
        return blurBuffer.getColorBuffer();
    }
}
