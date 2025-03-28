package cinnamon.vr;

import cinnamon.render.framebuffer.Framebuffer;
import org.lwjgl.openxr.XrRect2Di;
import org.lwjgl.openxr.XrSwapchainImageOpenGLKHR;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL30.*;

public class XrFramebuffer extends Framebuffer {

    private final Map<XrSwapchainImageOpenGLKHR, Integer> depthTextures = new HashMap<>(0);

    private int color, depth;

    public XrFramebuffer(XrManager.Swapchain[] swapchains, int w, int h) {
        super(w, h, Framebuffer.COLOR_BUFFER | Framebuffer.DEPTH_BUFFER);
        genBuffers(swapchains);
    }

    @Override
    protected void genBuffers() {
        //do not initialize in super
    }

    private void genBuffers(XrManager.Swapchain[] swapchains) {
        use();

        //depths
        for (XrManager.Swapchain swapchain : swapchains) {
            for (XrSwapchainImageOpenGLKHR swapchainImage : swapchain.images) {
                int texture = glGenTextures();
                glBindTexture(GL_TEXTURE_2D, texture);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
                glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT32, swapchain.width, swapchain.height, 0, GL_DEPTH_COMPONENT, GL_FLOAT, (ByteBuffer) null);
                depthTextures.put(swapchainImage, texture);
            }
        }

        //unbind stuff
        glBindTexture(GL_TEXTURE_2D, 0);
        DEFAULT_FRAMEBUFFER.use();
    }

    @Override
    protected void freeTextures() {
        for (int texture : depthTextures.values())
            glDeleteTextures(texture);
    }

    @Override
    public int getColorBuffer() {
        return color;
    }

    @Override
    public int getDepthBuffer() {
        return depth;
    }

    public void bindTextures(XrSwapchainImageOpenGLKHR swapchainImage) {
        color = swapchainImage.image();
        depth = depthTextures.get(swapchainImage);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, color, 0);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, depth, 0);
    }

    public void adjustViewPort(XrRect2Di imageRect) {
        setPos(imageRect.offset().x(), imageRect.offset().y());
        setSize(imageRect.extent().width(), imageRect.extent().height());
        adjustViewPort();
    }
}
