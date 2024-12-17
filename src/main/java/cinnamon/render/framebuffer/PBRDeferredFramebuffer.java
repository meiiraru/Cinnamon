package cinnamon.render.framebuffer;

import cinnamon.render.texture.Texture;

import static org.lwjgl.opengl.GL30.*;

public class PBRDeferredFramebuffer extends Framebuffer {

    /**
    gPosition  - pos (RGB)                             - GL_RGBA32F
    gAlbedo    - albedo (RGBA)                         - GL_RGBA
    gORM       - ao (R) + roughness (G) + metallic (B) - GL_RGBA
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

    public int gPosition, gAlbedo, gORM, gNormal, gEmissive;

    public PBRDeferredFramebuffer(int width, int height) {
        super(width, height, DEPTH_BUFFER | STENCIL_BUFFER);
    }

    @Override
    protected void genBuffers() {
        super.genBuffers();

        use();

        int width = getWidth();
        int height = getHeight();

        this.gPosition = genTexture(GL_RGBA32F, width, height, GL_RGBA, GL_FLOAT, GL_NEAREST, GL_REPEAT, ATTACHMENTS[0]);
        this.gAlbedo   = genTexture(GL_RGBA, width, height, GL_RGBA, GL_UNSIGNED_BYTE, GL_NEAREST, GL_REPEAT, ATTACHMENTS[1]);
        this.gORM      = genTexture(GL_RGBA, width, height, GL_RGBA, GL_UNSIGNED_BYTE, GL_NEAREST, GL_REPEAT, ATTACHMENTS[2]);
        this.gNormal   = genTexture(GL_RGBA32F, width, height, GL_RGBA, GL_FLOAT, GL_NEAREST, GL_REPEAT, ATTACHMENTS[3]);
        this.gEmissive = genTexture(GL_RGBA, width, height, GL_RGBA, GL_UNSIGNED_BYTE, GL_NEAREST, GL_REPEAT, ATTACHMENTS[4]);
        glDrawBuffers(ATTACHMENTS);

        glBindTexture(GL_TEXTURE_2D, 0);

        checkForErrors();
        DEFAULT_FRAMEBUFFER.use();
    }

    @Override
    protected void freeTextures() {
        super.freeTextures();
        glDeleteTextures(gPosition);
        glDeleteTextures(gAlbedo);
        glDeleteTextures(gORM);
        glDeleteTextures(gNormal);
        glDeleteTextures(gEmissive);
    }

    public int bindTextures() {
        Texture.bind(gPosition, 0);
        Texture.bind(gAlbedo, 1);
        Texture.bind(gORM, 2);
        Texture.bind(gNormal, 3);
        Texture.bind(gEmissive, 4);
        return 5;
    }
}
