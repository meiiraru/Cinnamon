package cinnamon.render;

import cinnamon.render.framebuffer.Framebuffer;
import cinnamon.render.shader.PostProcess;
import cinnamon.render.shader.Shader;
import cinnamon.render.texture.Texture;

import static cinnamon.render.WorldRenderer.renderQuad;
import static org.lwjgl.opengl.GL11.*;

public class Blur {

    public static int gaussianBlur(int texture, int width, int height, int blurRadius, Framebuffer pingPongA, Framebuffer pingPongB) {
        Framebuffer prevFramebuffer = Framebuffer.activeFramebuffer;
        int w = width / blurRadius;
        int h = height / blurRadius;

        pingPongA.resize(w, h);
        pingPongB.resize(w, h);
        pingPongA.useClear();
        pingPongB.useClear();

        Shader sh = PostProcess.GAUSSIAN_BLUR.getShader().use();
        sh.setVec2("texelSize", 1f / w, 1f / h);

        glDisable(GL_BLEND);

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

        glEnable(GL_BLEND);
        Texture.unbindAll(1);

        prevFramebuffer.use();
        prevFramebuffer.adjustViewPort();

        return tex;
    }

    public static int boxBlur(int texture, int width, int height, int blurRadius, Framebuffer blurBuffer) {
        Framebuffer prevFramebuffer = Framebuffer.activeFramebuffer;

        blurBuffer.resize(width, height);
        blurBuffer.useClear();
        blurBuffer.adjustViewPort();

        Shader sh = PostProcess.BOX_BLUR.getShader().use();
        sh.setTexture("colorTex", texture, 0);
        sh.setVec2("texelSize", 1f / width, 1f / height);
        sh.setInt("radius", blurRadius);

        glDisable(GL_BLEND);
        renderQuad();

        glEnable(GL_BLEND);
        Texture.unbindAll(1);

        prevFramebuffer.use();
        prevFramebuffer.adjustViewPort();

        return blurBuffer.getColorBuffer();
    }
}
