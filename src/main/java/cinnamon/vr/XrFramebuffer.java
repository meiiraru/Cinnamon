package cinnamon.vr;

import cinnamon.render.framebuffer.Framebuffer;
import org.lwjgl.openxr.XrSwapchainImageOpenGLKHR;

import static org.lwjgl.opengl.GL30.*;

public class XrFramebuffer extends Framebuffer {

    private int color;

    public XrFramebuffer() {
        super(1, 1, Framebuffer.COLOR_BUFFER);
    }

    @Override
    protected void genBuffers() {
        //do not need to generate buffers
    }

    @Override
    protected void freeTextures() {
        //no textures to free
    }

    @Override
    public int getColorBuffer() {
        return color;
    }

    public void bindColorTexture(XrSwapchainImageOpenGLKHR swapchainImage) {
        color = swapchainImage.image();
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, color, 0);
    }
}
