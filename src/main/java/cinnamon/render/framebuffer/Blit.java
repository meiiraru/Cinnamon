package cinnamon.render.framebuffer;

import cinnamon.model.SimpleGeometry;
import cinnamon.render.shader.PostProcess;
import cinnamon.render.shader.Shader;
import cinnamon.render.texture.Texture;

import java.util.function.BiFunction;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.glBindFramebuffer;

public class Blit {

    public static BiFunction<Framebuffer, Shader, Integer>
            DEPTH_UNIFORM = (fb, s) -> {
                s.setTexture("depthTex", fb.getDepthBuffer(), 0);
                return 1;
            },
            COLOR_UNIFORM = (fb, s) -> {
                s.setTexture("colorTex", fb.getColorBuffer(), 0);
                return 1;
            },
            COLOR_AND_DEPTH_UNIFORM = (fb, s) -> {
                s.setTexture("colorTex", fb.getColorBuffer(), 0);
                s.setTexture("depthTex", fb.getDepthBuffer(), 1);
                return 2;
            };

    public static void copy(Framebuffer source, int targetFramebufferID, PostProcess postProcess) {
        copy(source, targetFramebufferID, postProcess.getShader(), postProcess.uniformFunction());
    }

    public static void copy(Framebuffer source, int targetFramebufferID, Shader shader, BiFunction<Framebuffer, Shader, Integer> shaderUniforms) {
        //prepare framebuffer
        glBindFramebuffer(GL_FRAMEBUFFER, targetFramebufferID);

        //prepare shader
        int textures = shaderUniforms.apply(source, shader.use());

        //draw quad
        renderQuad();

        //blit
        source.blit(targetFramebufferID);

        //unbind
        Texture.unbindAll(textures);
    }

    public static void renderQuad() {
        glDisable(GL_DEPTH_TEST);
        SimpleGeometry.QUAD.render();
        glEnable(GL_DEPTH_TEST);
    }
}
