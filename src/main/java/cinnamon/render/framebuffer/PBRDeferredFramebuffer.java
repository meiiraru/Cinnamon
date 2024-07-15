package cinnamon.render.framebuffer;

import cinnamon.render.texture.Texture;

import static org.lwjgl.opengl.GL30.*;

public class PBRDeferredFramebuffer extends Framebuffer {

    /**
    gPosition  - pos (RGB)                             - GL_RGBA32F
    gAlbedo    - albedo (RGBA)                         - GL_RGBA
    gRMAo      - roughness (R) + metallic (G) + ao (B) - GL_RGBA
    gNormal    - normal (RGB) + TBN                    - GL_RGBA32F
    gEmissive  - emissive (RGB)                        - GL_RGBA
    **/

    private static final int[] ATTACHMENTS = {
            GL_COLOR_ATTACHMENT0,
            GL_COLOR_ATTACHMENT1,
            GL_COLOR_ATTACHMENT2,
            GL_COLOR_ATTACHMENT3,
            GL_COLOR_ATTACHMENT4,
    };

    private int rboDepth;
    public int gPosition, gAlbedo, gRMAo, gNormal, gEmissive;

    public PBRDeferredFramebuffer(int width, int height) {
        super(width, height, 0);
    }

    @Override
    protected void genBuffers() {
        use();

        int width = getWidth();
        int height = getHeight();

        this.gPosition = genTexture(GL_RGBA32F, width, height, GL_RGBA, GL_FLOAT, GL_NEAREST, GL_REPEAT, ATTACHMENTS[0]);
        this.gAlbedo   = genTexture(GL_RGBA, width, height, GL_RGBA, GL_UNSIGNED_BYTE, GL_NEAREST, GL_REPEAT, ATTACHMENTS[1]);
        this.gRMAo     = genTexture(GL_RGBA, width, height, GL_RGBA, GL_UNSIGNED_BYTE, GL_NEAREST, GL_REPEAT, ATTACHMENTS[2]);
        this.gNormal   = genTexture(GL_RGBA32F, width, height, GL_RGBA, GL_FLOAT, GL_NEAREST, GL_REPEAT, ATTACHMENTS[3]);
        this.gEmissive = genTexture(GL_RGBA, width, height, GL_RGBA, GL_UNSIGNED_BYTE, GL_NEAREST, GL_REPEAT, ATTACHMENTS[4]);
        glDrawBuffers(ATTACHMENTS);

        this.rboDepth = glGenRenderbuffers();
        glBindRenderbuffer(GL_RENDERBUFFER, rboDepth);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT, width, height);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, rboDepth);

        glBindTexture(GL_TEXTURE_2D, 0);

        checkForErrors();
        DEFAULT_FRAMEBUFFER.use();
    }

    @Override
    protected void freeTextures() {
        glDeleteRenderbuffers(rboDepth);
        glDeleteTextures(gPosition);
        glDeleteTextures(gAlbedo);
        glDeleteTextures(gRMAo);
        glDeleteTextures(gNormal);
        glDeleteTextures(gEmissive);
    }

    public int bindTextures() {
        Texture.bind(gPosition, 0);
        Texture.bind(gAlbedo, 1);
        Texture.bind(gRMAo, 2);
        Texture.bind(gNormal, 3);
        Texture.bind(gEmissive, 4);
        return 5;
    }
}
