package mayo.render.framebuffer;

import mayo.render.shader.Shader;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;

public class Blit {
    private static final int vao, vbo;
    private static final float[] plane = {
            //x    y     u   v
            -1f,  1f,    0f, 1f,
            -1f, -1f,    0f, 0f,
            1f, -1f,    1f, 0f,

            -1f,  1f,    0f, 1f,
            1f, -1f,    1f, 0f,
            1f,  1f,    1f, 1f
    };

    static {
        vao = glGenVertexArrays();
        vbo = glGenBuffers();

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);

        glBufferData(GL_ARRAY_BUFFER, plane, GL_STATIC_DRAW);

        int stride = Float.BYTES * 4;
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 2, GL_FLOAT, false, stride, 0);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, Float.BYTES * 2);
    }

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
        glBindVertexArray(vao);
        glDrawArrays(GL_TRIANGLES, 0, 6);

        //re-enable depth test
        glEnable(GL_DEPTH_TEST);
    }
}
