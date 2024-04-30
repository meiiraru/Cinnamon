package mayo.render.framebuffer;

import mayo.Client;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL30.*;

public class Framebuffer {

    public static final int
            COLOR_BUFFER = 0x1,
            DEPTH_BUFFER = 0x2,
            HDR_COLOR_BUFFER = 0x4;

    private final int flags;
    private final int fbo;
    private int color, depth;
    private int width, height;
    public static Framebuffer activeFramebuffer;

    public Framebuffer(int width, int height, int flags) {
        //generate and use a new framebuffer
        this.fbo = glGenFramebuffers();
        this.width = width;
        this.height = height;
        this.flags = flags;
        genBuffers();
    }

    private void genBuffers() {
        use();

        boolean hasColorBuffer = (flags & COLOR_BUFFER) == COLOR_BUFFER;
        boolean hasDepthBuffer = (flags & DEPTH_BUFFER) == DEPTH_BUFFER;
        boolean hdrColorBuffer = (flags & HDR_COLOR_BUFFER) == HDR_COLOR_BUFFER;

        //color buffer
        if (hasColorBuffer) {
            this.color = genTexture(width, height, GL_RGBA, GL_RGBA, GL_UNSIGNED_BYTE, GL_LINEAR, GL_CLAMP_TO_EDGE, GL_COLOR_ATTACHMENT0);
        } else if (hdrColorBuffer) {
            this.color = genTexture(width, height, GL_RGBA16F, GL_RGBA, GL_FLOAT, GL_LINEAR, GL_CLAMP_TO_EDGE, GL_COLOR_ATTACHMENT0);
        } else {
            glDrawBuffer(GL_NONE);
            glReadBuffer(GL_NONE);
        }

        //depth buffer
        if (hasDepthBuffer) {
            this.depth = genTexture(width, height, GL_DEPTH_COMPONENT, GL_DEPTH_COMPONENT, GL_FLOAT, GL_NEAREST, GL_CLAMP_TO_BORDER, GL_DEPTH_ATTACHMENT);
            glTexParameterfv(GL_TEXTURE_2D, GL_TEXTURE_BORDER_COLOR, new float[]{1, 1, 1, 1});
        }

        //unbind textures
        glBindTexture(GL_TEXTURE_2D, 0);

        //check for completeness
        int status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
        if (status != GL_FRAMEBUFFER_COMPLETE)
            throw new RuntimeException("Framebuffer is not complete! " + status);

        //unbind this
        useDefault();
    }

    private static int genTexture(int width, int height, int internalFormat, int format, int type, int filter, int warp, int attachment) {
        int texture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, texture);
        glTexImage2D(GL_TEXTURE_2D, 0, internalFormat, width, height, 0, format, type, (ByteBuffer) null);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, filter);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, filter);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, warp);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, warp);

        glFramebufferTexture2D(GL_FRAMEBUFFER, attachment, GL_TEXTURE_2D, texture, 0);

        return texture;
    }

    public static void useDefault() {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        Client c = Client.getInstance();
        glViewport(0, 0, c.window.width, c.window.height);
        activeFramebuffer = null;
    }

    public void use() {
        glBindFramebuffer(GL_FRAMEBUFFER, this.fbo);
        glViewport(0, 0, width, height);
        activeFramebuffer = this;
    }

    public void clear() {
        glClearColor(0f, 0f, 0f, 0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT /* | GL_STENCIL_BUFFER_BIT */);
    }

    private void freeTextures() {
        if (color > 0)
            glDeleteTextures(this.color);
        if (depth > 0)
            glDeleteTextures(this.depth);
    }

    public void free() {
        glDeleteFramebuffers(this.fbo);
        freeTextures();
    }

    public int getColorBuffer() {
        return color;
    }

    public int getDepthBuffer() {
        return depth;
    }

    public void resize(int width, int height) {
        this.width = width;
        this.height = height;
        freeTextures();
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
