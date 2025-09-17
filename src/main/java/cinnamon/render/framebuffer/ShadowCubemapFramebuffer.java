package cinnamon.render.framebuffer;

import cinnamon.render.texture.CubeMap;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL12.GL_TEXTURE_WRAP_R;
import static org.lwjgl.opengl.GL13.GL_TEXTURE_CUBE_MAP;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class ShadowCubemapFramebuffer extends Framebuffer {

    private int depthCube;

    public ShadowCubemapFramebuffer() {
        super(0);
    }

    @Override
    protected void genBuffers() {
        use();

        int width = getWidth();
        int height = getHeight();

        //create cubemap texture
        depthCube = glGenTextures();
        glBindTexture(GL_TEXTURE_CUBE_MAP, depthCube);

        for (CubeMap.Face value : CubeMap.Face.values())
            glTexImage2D(value.GLTarget, 0, GL_DEPTH_COMPONENT24, width, height, 0, GL_DEPTH_COMPONENT, GL_UNSIGNED_INT, NULL);

        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE);
        glTexParameterfv(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_BORDER_COLOR, new float[]{1, 1, 1, 1});

        //set depth to the cubemap
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_CUBE_MAP_POSITIVE_X, depthCube, 0);

        //finish using super
        super.genBuffers();
    }

    @Override
    protected void freeTextures() {
        super.freeTextures();
        glDeleteTextures(depthCube);
    }

    public void bindCubemap(int texTarget) {
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, texTarget, depthCube, 0);
        clear();
    }

    public int getCubemap() {
        return depthCube;
    }
}
