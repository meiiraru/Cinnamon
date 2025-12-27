package cinnamon.render.framebuffer;

import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Framebuffer {

    public static final int
            COLOR_BUFFER = 0x1,
            DEPTH_BUFFER = 0x2,
            STENCIL_BUFFER = 0x4,
            HDR_COLOR_BUFFER = 0x8;

    protected final int flags;
    protected final int clearMask;
    private int fbo;
    private int color, depth, stencil;
    private int x, y;
    private int width, height;

    public static final Framebuffer DEFAULT_FRAMEBUFFER = new Framebuffer(COLOR_BUFFER | DEPTH_BUFFER | STENCIL_BUFFER);

    public static Framebuffer activeFramebuffer;

    public Framebuffer(int flags) {
        this.flags = flags;
        int clearMask = 0;
        if ((flags & COLOR_BUFFER) != 0 || (flags & HDR_COLOR_BUFFER) != 0) clearMask |= GL_COLOR_BUFFER_BIT;
        if ((flags & DEPTH_BUFFER) != 0) clearMask |= GL_DEPTH_BUFFER_BIT;
        if ((flags & STENCIL_BUFFER) != 0) clearMask |= GL_STENCIL_BUFFER_BIT;
        this.clearMask = clearMask;
    }

    protected Framebuffer(int flags, int clearMask) {
        this.flags = flags;
        this.clearMask = clearMask;
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
            this.depth = genTexture(GL_DEPTH24_STENCIL8, width, height, GL_DEPTH_STENCIL, GL_UNSIGNED_INT_24_8, GL_NEAREST, GL_CLAMP_TO_BORDER, GL_DEPTH_STENCIL_ATTACHMENT);
            glTexParameterfv(GL_TEXTURE_2D, GL_TEXTURE_BORDER_COLOR, new float[]{1, 1, 1, 1});
        }
        else {
            if (hasDepthBuffer) {
                this.depth = genTexture(GL_DEPTH_COMPONENT24, width, height, GL_DEPTH_COMPONENT, GL_UNSIGNED_INT, GL_NEAREST, GL_CLAMP_TO_BORDER, GL_DEPTH_ATTACHMENT);
                glTexParameterfv(GL_TEXTURE_2D, GL_TEXTURE_BORDER_COLOR, new float[]{1, 1, 1, 1});
            }
            if (hasStencilBuffer) {
                this.stencil = genTexture(GL_STENCIL_INDEX8, width, height, GL_STENCIL_INDEX, GL_UNSIGNED_BYTE, GL_NEAREST, GL_CLAMP_TO_BORDER, GL_STENCIL_ATTACHMENT);
                glTexParameterfv(GL_TEXTURE_2D, GL_TEXTURE_BORDER_COLOR, new float[]{1, 1, 1, 1});
            }
        }

        //unbind textures
        glBindTexture(GL_TEXTURE_2D, 0);

        //check for completeness
        checkForErrors();

        //unbind this
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

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, filter);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, filter);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, warp);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, warp);

        glFramebufferTexture2D(GL_FRAMEBUFFER, attachment, GL_TEXTURE_2D, texture, 0);

        return texture;
    }

    public Framebuffer use() {
        if (this.fbo == 0) {
            this.fbo = glGenFramebuffers();
            genBuffers();
        }

        glBindFramebuffer(GL_FRAMEBUFFER, this.fbo);
        activeFramebuffer = this;
        return this;
    }

    public void adjustViewPort() {
        glViewport(x, y, width, height);
    }

    public void clear() {
        if (clearMask == 0)
            return;

        glClearColor(0f, 0f, 0f, 0f);
        glClear(clearMask);
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
        if (this.fbo == 0)
            return;

        this.fbo = 0;
        if (activeFramebuffer == this)
            glBindFramebuffer(GL_FRAMEBUFFER, 0);

        glDeleteFramebuffers(this.fbo);
        freeTextures();
    }

    public void blit(int targetFramebuffer) {
        int mask = getBlitMask((flags & COLOR_BUFFER) != 0 || (flags & HDR_COLOR_BUFFER) != 0, (flags & DEPTH_BUFFER) != 0, (flags & STENCIL_BUFFER) != 0);
        blit(id(), targetFramebuffer, getX(), getY(), getWidth(), getHeight(), getX(), getY(), getWidth(), getHeight(), mask, GL_NEAREST);
    }

    public void blit(Framebuffer targetFramebuffer) {
        this.blit(targetFramebuffer, (flags & COLOR_BUFFER) != 0 || (flags & HDR_COLOR_BUFFER) != 0, (flags & DEPTH_BUFFER) != 0, (flags & STENCIL_BUFFER) != 0);
    }

    public void blit(Framebuffer targetFramebuffer, boolean color, boolean depth, boolean stencil) {
        int mask = getBlitMask(color, depth, stencil);
        blit(id(), targetFramebuffer.id(),
                this.getX(), this.getY(), this.getWidth(), this.getHeight(),
                targetFramebuffer.getX(), targetFramebuffer.getY(), targetFramebuffer.getWidth(), targetFramebuffer.getHeight(),
                mask, GL_NEAREST
        );
    }

    public static int getBlitMask(boolean color, boolean depth, boolean stencil) {
        int mask = 0;
        if (color) mask |= GL_COLOR_BUFFER_BIT;
        if (depth) mask |= GL_DEPTH_BUFFER_BIT;
        if (stencil) mask |= GL_STENCIL_BUFFER_BIT;
        return mask;
    }

    public static void blit(int source, int target, int x0, int y0, int w0, int h0, int x1, int y1, int w1, int h1, int mask, int filter) {
        if (source == target || mask == 0)
            return;

        glBindFramebuffer(GL_READ_FRAMEBUFFER, source);
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, target);
        glBlitFramebuffer(x0, y0, w0, h0, x1, y1, w1, h1, mask, filter);
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
    public void resizeTo(Framebuffer other, float scale) {
        resize((int) (other.getWidth() * scale), (int) (other.getHeight() * scale));
    }

    public void resize(int width, int height) {
        if (width == this.width && height == this.height)
            return;

        this.width = width; this.height = height;
        free();
    }

    public void setPos(int x, int y) {
        this.x = x;
        this.y = y;
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
