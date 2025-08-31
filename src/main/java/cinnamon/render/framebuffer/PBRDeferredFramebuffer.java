package cinnamon.render.framebuffer;

import static org.lwjgl.opengl.GL30.*;

public class PBRDeferredFramebuffer extends Framebuffer {

    /**
     gAlbedo    - albedo (RGBA)                         - GL_RGBA
     gPosition  - pos (RGB)                             - GL_RGBA32F
     gNormal    - normal (RGB) + TBN                    - GL_RGBA32F
     gORM       - ao (R) + roughness (G) + metallic (B) - GL_RGBA
     gEmissive  - emissive (RGB)                        - GL_RGBA
    **/

    private static final int[] ATTACHMENTS = {
            GL_COLOR_ATTACHMENT0,
            GL_COLOR_ATTACHMENT1,
            GL_COLOR_ATTACHMENT2,
            GL_COLOR_ATTACHMENT3,
            GL_COLOR_ATTACHMENT4,
    };

    public int gAlbedo, gPosition, gNormal, gORM, gEmissive;

    public PBRDeferredFramebuffer() {
        super(DEPTH_BUFFER | STENCIL_BUFFER);
    }

    @Override
    protected void genBuffers() {
        super.genBuffers();
        use();

        int width = getWidth();
        int height = getHeight();

        this.gAlbedo   = genTexture(GL_RGBA,    width, height, GL_RGBA, GL_UNSIGNED_BYTE, GL_NEAREST, GL_REPEAT, ATTACHMENTS[0]);
        this.gPosition = genTexture(GL_RGBA32F, width, height, GL_RGBA, GL_FLOAT,         GL_NEAREST, GL_REPEAT, ATTACHMENTS[1]);
        this.gNormal   = genTexture(GL_RGBA32F, width, height, GL_RGBA, GL_FLOAT,         GL_NEAREST, GL_REPEAT, ATTACHMENTS[2]);
        this.gORM      = genTexture(GL_RGBA,    width, height, GL_RGBA, GL_UNSIGNED_BYTE, GL_NEAREST, GL_REPEAT, ATTACHMENTS[3]);
        this.gEmissive = genTexture(GL_RGBA,    width, height, GL_RGBA, GL_UNSIGNED_BYTE, GL_NEAREST, GL_REPEAT, ATTACHMENTS[4]);
        glDrawBuffers(ATTACHMENTS);

        glBindTexture(GL_TEXTURE_2D, 0);

        checkForErrors();
        DEFAULT_FRAMEBUFFER.use();
    }

    @Override
    protected void freeTextures() {
        super.freeTextures();
        glDeleteTextures(gAlbedo);
        glDeleteTextures(gPosition);
        glDeleteTextures(gNormal);
        glDeleteTextures(gORM);
        glDeleteTextures(gEmissive);
    }

    public int getTexture(int index) {
        return switch (index) {
            case 0 -> gAlbedo;
            case 1 -> gPosition;
            case 2 -> gNormal;
            case 3 -> gORM;
            case 4 -> gEmissive;
            default -> 0;
        };
    }
}
