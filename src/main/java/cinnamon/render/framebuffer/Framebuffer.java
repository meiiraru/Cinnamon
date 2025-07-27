package cinnamon.render.framebuffer;

import cinnamon.Client;
import cinnamon.render.Window;

import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Framebuffer {

    public static final int
            COLOR_BUFFER = 0x1,
            DEPTH_BUFFER = 0x2,
            STENCIL_BUFFER = 0x4,
            HDR_COLOR_BUFFER = 0x8;

    private final int flags;
    private final int fbo;
    private int color, depth, stencil;
    private int x, y;
    private int width, height;

    public static final Framebuffer DEFAULT_FRAMEBUFFER;

    public static Framebuffer activeFramebuffer;

    static {
        Window w = Client.getInstance().window;
        DEFAULT_FRAMEBUFFER = new Framebuffer(w.width, w.height, COLOR_BUFFER | DEPTH_BUFFER | STENCIL_BUFFER);
    }

    public Framebuffer(int width, int height, int flags) {
        //generate and use a new framebuffer
        this.fbo = glGenFramebuffers();
        this.width = width;
        this.height = height;
        this.flags = flags;
        genBuffers();
    }

    protected void genBuffers() {
        use();

        boolean hasColorBuffer = (flags & COLOR_BUFFER) != 0;
        boolean hdrColorBuffer = (flags & HDR_COLOR_BUFFER) != 0;
        boolean hasDepthBuffer = (flags & DEPTH_BUFFER) != 0;
        boolean hasStencilBuffer = (flags & STENCIL_BUFFER) != 0;

        //color buffer
        if (hasColorBuffer) {
            this.color = genTexture(GL_RGBA, width, height, GL_RGBA, GL_UNSIGNED_BYTE, GL_LINEAR, GL_CLAMP_TO_EDGE, GL_COLOR_ATTACHMENT0);
        } else if (hdrColorBuffer) {
            this.color = genTexture(GL_RGBA16F, width, height, GL_RGBA, GL_FLOAT, GL_LINEAR, GL_CLAMP_TO_EDGE, GL_COLOR_ATTACHMENT0);
        } else {
            glDrawBuffer(GL_NONE);
            glReadBuffer(GL_NONE);
        }

        //depth / stencil buffer
        if (hasDepthBuffer && hasStencilBuffer) {
            this.depth = genTexture(GL_DEPTH24_STENCIL8, width, height, GL_DEPTH_STENCIL, GL_UNSIGNED_INT_24_8, GL_NEAREST, GL_CLAMP_TO_EDGE, GL_DEPTH_STENCIL_ATTACHMENT);
            glTexParameterfv(GL_TEXTURE_2D, GL_TEXTURE_BORDER_COLOR, new float[]{0, 1, 0, 1});
        }
        else {
            if (hasDepthBuffer) {
                this.depth = genTexture(GL_DEPTH_COMPONENT24, width, height, GL_DEPTH_COMPONENT, GL_UNSIGNED_INT, GL_NEAREST, GL_CLAMP_TO_EDGE, GL_DEPTH_ATTACHMENT);
                glTexParameterfv(GL_TEXTURE_2D, GL_TEXTURE_BORDER_COLOR, new float[]{0, 1, 0, 0});
            }
            if (hasStencilBuffer) {
                this.stencil = genTexture(GL_STENCIL_INDEX8, width, height, GL_STENCIL_INDEX, GL_UNSIGNED_BYTE, GL_NEAREST, GL_CLAMP_TO_EDGE, GL_STENCIL_ATTACHMENT);
                glTexParameterfv(GL_TEXTURE_2D, GL_TEXTURE_BORDER_COLOR, new float[]{0, 1, 0, 0});
            }
        }

        //unbind textures
        glBindTexture(GL_TEXTURE_2D, 0);

        //check for completeness
        checkForErrors();

        //unbind this - unless when we're creating the default framebuffer
        if (DEFAULT_FRAMEBUFFER != null)
            DEFAULT_FRAMEBUFFER.use();
    }

    protected static void checkForErrors() {
        int status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
        if (status != GL_FRAMEBUFFER_COMPLETE)
            throw new RuntimeException("Framebuffer is not complete! " + status);
    }

    protected static int genTexture(int internalFormat, int width, int height, int format, int type, int filter, int warp, int attachment) {
        int texture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, texture);
        glTexImage2D(GL_TEXTURE_2D, 0, internalFormat, width, height, 0, format, type, NULL);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, filter);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, filter);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, warp);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, warp);

        glFramebufferTexture2D(GL_FRAMEBUFFER, attachment, GL_TEXTURE_2D, texture, 0);

        return texture;
    }

    public Framebuffer use() {
        glBindFramebuffer(GL_FRAMEBUFFER, this.fbo);
        activeFramebuffer = this;
        return this;
    }

    public void adjustViewPort() {
        glViewport(x, y, width, height);
    }

    public static void clear() {
        glClearColor(0f, 0f, 0f, 0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);
    }

    public void useClear() {
        use();
        clear();
    }

    protected void freeTextures() {
        if (color > 0)
            glDeleteTextures(this.color);
        if (depth > 0)
            glDeleteTextures(this.depth);
        if (stencil > 0)
            glDeleteTextures(this.stencil);
    }

    public void free() {
        glDeleteFramebuffers(this.fbo);
        freeTextures();
    }

    public void blit(int targetFramebuffer) {
        this.blit(targetFramebuffer, (flags & COLOR_BUFFER) != 0 || (flags & HDR_COLOR_BUFFER) != 0, (flags & DEPTH_BUFFER) != 0, (flags & STENCIL_BUFFER) != 0);
    }

    public void blit(int targetFramebuffer, boolean color, boolean depth, boolean stencil) {
        glBindFramebuffer(GL_READ_FRAMEBUFFER, id());
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, targetFramebuffer);
        if (color) glBlitFramebuffer(getX(), getY(), getWidth(), getHeight(), getX(), getY(), getWidth(), getHeight(), GL_COLOR_BUFFER_BIT, GL_NEAREST);
        if (depth) glBlitFramebuffer(getX(), getY(), getWidth(), getHeight(), getX(), getY(), getWidth(), getHeight(), GL_DEPTH_BUFFER_BIT, GL_NEAREST);
        if (stencil) glBlitFramebuffer(getX(), getY(), getWidth(), getHeight(), getX(), getY(), getWidth(), getHeight(), GL_STENCIL_BUFFER_BIT, GL_NEAREST);
        glBindFramebuffer(GL_FRAMEBUFFER, targetFramebuffer);
    }

    public int getColorBuffer() {
        return color;
    }

    public int getDepthBuffer() {
        return depth;
    }

    public int getStencilBuffer() {
        return (flags & DEPTH_BUFFER) != 0 ? depth : stencil;
    }

    public void resizeTo(Framebuffer other) {
        resize(other.getWidth(), other.getHeight());
    }

    public void resize(int width, int height) {
        if (width == this.width && height == this.height)
            return;
        setSize(width, height);
        freeTextures();
        genBuffers();
    }

    public void setPos(int x, int y) {
        this.x = x;
        this.y = y;
    }

    protected void setSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int id() {
        return this.fbo;
    }
}
