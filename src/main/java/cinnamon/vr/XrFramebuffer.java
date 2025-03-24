package cinnamon.vr;

import cinnamon.render.framebuffer.Framebuffer;
import org.lwjgl.openxr.XrRect2Di;
import org.lwjgl.openxr.XrSwapchainImageOpenGLKHR;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL30.*;

public class XrFramebuffer {

    private final int fbo;
    private final Map<XrSwapchainImageOpenGLKHR, Integer> depthTextures = new HashMap<>(0);

    public XrFramebuffer(XrManager.Swapchain[] swapchains) {
        this.fbo = glGenFramebuffers();
        for (XrManager.Swapchain swapchain : swapchains) {
            for (XrSwapchainImageOpenGLKHR swapchainImage : swapchain.images) {
                int texture = glGenTextures();
                glBindTexture(GL_TEXTURE_2D, texture);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
                glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT32, swapchain.width, swapchain.height, 0, GL_DEPTH_COMPONENT, GL_FLOAT, (ByteBuffer)null);
                depthTextures.put(swapchainImage, texture);
            }
        }

        glBindTexture(GL_TEXTURE_2D, 0);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    public void use() {
        glBindFramebuffer(GL_FRAMEBUFFER, fbo);
    }

    public void clear() {
        Framebuffer.clear();
    }

    public void free() {
        for (int texture : depthTextures.values())
            glDeleteTextures(texture);
        glDeleteFramebuffers(fbo);
    }

    public void bindTextures(XrSwapchainImageOpenGLKHR swapchainImage) {
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, swapchainImage.image(), 0);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, depthTextures.get(swapchainImage), 0);
    }

    public void adjustViewPort(XrRect2Di imageRect) {
        glViewport(
                imageRect.offset().x(),
                imageRect.offset().y(),
                imageRect.extent().width(),
                imageRect.extent().height()
        );
    }
}
