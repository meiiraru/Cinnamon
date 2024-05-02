package mayo.render.framebuffer;

import mayo.model.SimpleGeometry;
import mayo.render.shader.Shader;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.glBindFramebuffer;

public class Blit {
    private static final SimpleGeometry QUAD = SimpleGeometry.quad();

    public static void copy(Framebuffer source, int targetFramebufferID, Shader shader) {
        //use framebuffer
        glBindFramebuffer(GL_FRAMEBUFFER, targetFramebufferID);

        //prepare shader
        shader.use();
        shader.setVec2("textelSize", 1f / source.getWidth(), 1f / source.getHeight());

        //textures
        shader.setInt("screenTexture", 0);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, source.getColorBuffer());

        shader.setInt("depthTexture", 1);
        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, source.getDepthBuffer());

        //disable depth
        glDisable(GL_DEPTH_TEST);

        //draw
        QUAD.render();

        //re-enable depth test
        glEnable(GL_DEPTH_TEST);
    }
}
