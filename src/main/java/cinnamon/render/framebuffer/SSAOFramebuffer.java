package cinnamon.render.framebuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL30.*;

public class SSAOFramebuffer extends Framebuffer {

    private int texture;

    public SSAOFramebuffer() {
        super(0, GL_COLOR_BUFFER_BIT);
    }

    @Override
    protected void genBuffers() {
        use();

        texture = genTexture(GL_RED, getWidth(), getHeight(), GL_RED, GL_FLOAT, GL_LINEAR, GL_CLAMP_TO_EDGE, GL_COLOR_ATTACHMENT0);
        glBindTexture(GL_TEXTURE_2D, 0);

        checkForErrors();
        DEFAULT_FRAMEBUFFER.use();
    }

    @Override
    protected void freeTextures() {
        super.freeTextures();
        glDeleteTextures(texture);
    }

    public int getTexture() {
        return texture;
    }
}
