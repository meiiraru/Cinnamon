package mayo.render.framebuffer;

import mayo.model.SimpleGeometry;
import mayo.render.shader.Shader;
import mayo.render.texture.Texture;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.glBindFramebuffer;

public class Blit {

    public static void copy(Framebuffer source, int targetFramebufferID, Shader shader) {
        //prepare framebuffer
        glBindFramebuffer(GL_FRAMEBUFFER, targetFramebufferID);

        //prepare shader
        prepareShader(source, shader);

        //draw quad
        renderQuad();

        //blit
        source.blit(targetFramebufferID);

        //unbind
        unbindTextures();
    }

    public static void prepareShader(Framebuffer source, Shader shader) {
        //prepare shader
        Shader s = shader.use();
        s.setVec2("textelSize", 1f / source.getWidth(), 1f / source.getHeight());

        s.setInt("screenTexture", 0);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, source.getColorBuffer());

        s.setInt("depthTexture", 1);
        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, source.getDepthBuffer());
    }

    public static void renderQuad() {
        glDisable(GL_DEPTH_TEST);
        SimpleGeometry.QUAD.render();
        glEnable(GL_DEPTH_TEST);
    }

    public static void unbindTextures() {
        Texture.unbindAll(2);
    }
}
