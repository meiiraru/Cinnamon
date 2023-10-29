package mayo.render;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL30.*;

public class Framebuffer {

    private final int fbo;
    private int color, depthStencil;
    private int width, height;

    public Framebuffer(int width, int height) {
        //generate and use a new framebuffer
        this.fbo = glGenFramebuffers();
        this.width = width;
        this.height = height;
        genBuffers();
    }

    private void genBuffers() {
        use();

        //color buffer
        this.color = genTexture(width, height, GL_RGBA, GL_RGBA, GL_UNSIGNED_BYTE, GL_COLOR_ATTACHMENT0);
        //depth/stencil buffer
        this.depthStencil = genTexture(width, height, GL_DEPTH24_STENCIL8, GL_DEPTH_STENCIL, GL_UNSIGNED_INT_24_8, GL_DEPTH_STENCIL_ATTACHMENT);

        //check for completeness
        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE)
            throw new RuntimeException("Framebuffer is not complete!");

        //unbind this
        useDefault();
    }

    private static int genTexture(int width, int height, int internalFormat, int format, int type, int attachment) {
        int texture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, texture);
        glTexImage2D(GL_TEXTURE_2D, 0, internalFormat, width, height, 0, format, type, (ByteBuffer) null);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

        glFramebufferTexture2D(GL_FRAMEBUFFER, attachment, GL_TEXTURE_2D, texture, 0);

        glBindTexture(GL_TEXTURE_2D, 0);
        return texture;
    }

    public void use() {
        glBindFramebuffer(GL_FRAMEBUFFER, this.fbo);
    }

    public void free() {
        glDeleteFramebuffers(this.fbo);
        glDeleteTextures(this.color);
        glDeleteRenderbuffers(this.depthStencil);
    }

    public static void useDefault() {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    public int getColorBuffer() {
        return color;
    }

    public int getDepthStencilBuffer() {
        return depthStencil;
    }

    public void resize(int width, int height) {
        this.width = width;
        this.height = height;
        glDeleteTextures(this.color);
        glDeleteRenderbuffers(this.depthStencil);
        genBuffers();
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int id() {
        return this.fbo;
    }
}
