package cinnamon.render.framebuffer;

import static org.lwjgl.opengl.GL30.*;

public class PBRDeferredFramebuffer extends Framebuffer {

    /**
     gAlbedo    - albedo (RGBA)                         - GL_RGBA
     gNormal    - normal (RGB) + TBN                    - GL_RGB16F
     gORM       - ao (R) + roughness (G) + metallic (B) - GL_RGB
     gEmissive  - emissive (RGB)                        - GL_RGB
    **/

    private static final int[] ATTACHMENTS = {
            GL_COLOR_ATTACHMENT0,
            GL_COLOR_ATTACHMENT1,
            GL_COLOR_ATTACHMENT2,
            GL_COLOR_ATTACHMENT3,
    };

    public int gAlbedo, gNormal, gORM, gEmissive;

    public PBRDeferredFramebuffer() {
        super(DEPTH_BUFFER | STENCIL_BUFFER, GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);
    }

    @Override
    protected void genBuffers() {
        super.genBuffers();
        use();

        int width = getWidth();
        int height = getHeight();

        this.gAlbedo   = genTexture(GL_RGBA,   width, height, GL_RGBA, GL_UNSIGNED_BYTE, GL_NEAREST, GL_REPEAT, ATTACHMENTS[0]);
        this.gNormal   = genTexture(GL_RGB16F, width, height, GL_RGB,  GL_FLOAT,         GL_NEAREST, GL_REPEAT, ATTACHMENTS[1]);
        this.gORM      = genTexture(GL_RGB,    width, height, GL_RGB,  GL_UNSIGNED_BYTE, GL_NEAREST, GL_REPEAT, ATTACHMENTS[2]);
        this.gEmissive = genTexture(GL_RGB,    width, height, GL_RGB,  GL_UNSIGNED_BYTE, GL_NEAREST, GL_REPEAT, ATTACHMENTS[3]);
        glDrawBuffers(ATTACHMENTS);

        glBindTexture(GL_TEXTURE_2D, 0);

        checkForErrors();
        DEFAULT_FRAMEBUFFER.use();
    }

    @Override
    protected void freeTextures() {
        super.freeTextures();
        glDeleteTextures(gAlbedo);
        glDeleteTextures(gNormal);
        glDeleteTextures(gORM);
        glDeleteTextures(gEmissive);
    }

    public int getAlbedo() {
        return gAlbedo;
    }

    public int getNormal() {
        return gNormal;
    }

    public int getORM() {
        return gORM;
    }

    public int getEmissive() {
        return gEmissive;
    }

    public int getTexture(int index) {
        return switch (index) {
            case 0 -> gAlbedo;
            case 1 -> gNormal;
            case 2 -> gORM;
            case 3 -> gEmissive;
            default -> 0;
        };
    }
}
