package mayo.render.framebuffer;

import mayo.model.SimpleGeometry;
import mayo.render.shader.PostProcess;
import mayo.render.shader.Shader;
import mayo.render.texture.Texture;

import java.util.function.BiFunction;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.glBindFramebuffer;

public class Blit {

    public static BiFunction<Framebuffer, Shader, Integer>
            DEPTH_ONLY_UNIFORM = (fb, s) -> {
                s.setInt("depthTexture", 0);
                glActiveTexture(GL_TEXTURE0);
                glBindTexture(GL_TEXTURE_2D, fb.getDepthBuffer());
                return 1;
            },
            SCREEN_TEX_ONLY_UNIFORM = (fb, s) -> {
                s.setInt("screenTexture", 0);
                glActiveTexture(GL_TEXTURE0);
                glBindTexture(GL_TEXTURE_2D, fb.getColorBuffer());
                return 1;
            };

    public static void copy(Framebuffer source, int targetFramebufferID, PostProcess postProcess) {
        copy(source, targetFramebufferID, postProcess.getShader(), postProcess.uniformFunction());
    }

    public static void copy(Framebuffer source, int targetFramebufferID, Shader shader, BiFunction<Framebuffer, Shader, Integer> shaderUniforms) {
        //prepare framebuffer
        glBindFramebuffer(GL_FRAMEBUFFER, targetFramebufferID);

        //prepare shader
        int textures = prepareShader(source, shader, shaderUniforms);

        //draw quad
        renderQuad();

        //blit
        source.blit(targetFramebufferID);

        //unbind
        Texture.unbindAll(textures);
    }

    public static int prepareShader(Framebuffer source, Shader shader, BiFunction<Framebuffer, Shader, Integer> shaderUniforms) {
        //prepare shader
        return shaderUniforms.apply(source, shader.use());
    }

    public static void renderQuad() {
        glDisable(GL_DEPTH_TEST);
        SimpleGeometry.QUAD.render();
        glEnable(GL_DEPTH_TEST);
    }
}
