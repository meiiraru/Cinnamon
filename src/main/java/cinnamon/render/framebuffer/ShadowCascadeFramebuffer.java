package cinnamon.render.framebuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.glTexImage3D;
import static org.lwjgl.opengl.GL13.GL_CLAMP_TO_BORDER;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL32.glFramebufferTexture;
import static org.lwjgl.system.MemoryUtil.NULL;

public class ShadowCascadeFramebuffer extends Framebuffer {

    private final int layers;
    private int depthTextureArray;

    public ShadowCascadeFramebuffer(int layers) {
        super(0, GL_DEPTH_BUFFER_BIT);
        this.layers = layers;
    }

    @Override
    protected void genBuffers() {
        use();

        int width = getWidth();
        int height = getHeight();

        //create texture array
        depthTextureArray = glGenTextures();
        glBindTexture(GL_TEXTURE_2D_ARRAY, depthTextureArray);
        glTexImage3D(GL_TEXTURE_2D_ARRAY, 0, GL_DEPTH_COMPONENT32F, width, height, layers, 0, GL_DEPTH_COMPONENT, GL_FLOAT, NULL);

        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER);
        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_BORDER);

        glTexParameterfv(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_BORDER_COLOR, new float[]{1, 1, 1, 1});

        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_COMPARE_MODE, GL_COMPARE_REF_TO_TEXTURE);
        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_COMPARE_FUNC, GL_GREATER);

        //bind texture array to framebuffer
        glFramebufferTexture(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, depthTextureArray, 0);
        glBindTexture(GL_TEXTURE_2D_ARRAY, 0);

        //finish with super (voiding r/w buffers, error checking)
        super.genBuffers();
    }

    @Override
    protected void freeTextures() {
        super.freeTextures();
        glDeleteTextures(depthTextureArray);
    }

    public int getDepthTextureArray() {
        return depthTextureArray;
    }

    public int getLayerCount() {
        return layers;
    }
}
