package cinnamon.render;

import cinnamon.model.SimpleGeometry;
import cinnamon.render.framebuffer.Framebuffer;
import cinnamon.render.shader.Shader;
import cinnamon.render.shader.Shaders;
import cinnamon.render.texture.CubeMap;
import cinnamon.render.texture.Texture;
import org.joml.Math;
import org.joml.Matrix4f;

import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT0;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.system.MemoryUtil.NULL;

public class CubemapRenderer {

    private static final int captureFBO = glGenFramebuffers();
    public static final Matrix4f CAPTURE_PROJECTION = new Matrix4f().perspective(Math.toRadians(90f), 1f, 0.1f, 10f);

    public static void renderInvertedCube(CubeMap cubemap, Shader shader) {
        Framebuffer oldFB = Framebuffer.activeFramebuffer;

        glBindFramebuffer(GL_FRAMEBUFFER, captureFBO);
        glViewport(0, 0, cubemap.getWidth(), cubemap.getHeight());
        shader.setMat4("projection", CAPTURE_PROJECTION);

        for (CubeMap.Face face : CubeMap.Face.values()) {
            shader.setMat4("view", face.viewMatrix);
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, face.GLTarget, cubemap.getID(), 0);
            glClear(GL_COLOR_BUFFER_BIT);
            SimpleGeometry.INV_CUBE.render();
        }

        oldFB.use();
        oldFB.adjustViewPort();
    }

    public static CubeMap hdrToCubemap(Texture texture, boolean hdr) {
        CubeMap cubemap = generateEmptyMap(512, 512, false, false);

        Shader old = Shader.activeShader;
        Shader s = Shaders.EQUIRECTANGULAR_TO_CUBEMAP.getShader().use();
        s.setTexture("equirectangularMap", texture, 0);
        s.setBool("hdr", hdr);

        renderInvertedCube(cubemap, s);

        old.use();
        return cubemap;
    }

    public static CubeMap generateIrradianceMap(CubeMap cubemap) {
        CubeMap irradiance = generateEmptyMap(32, 32, false, false);

        Shader old = Shader.activeShader;
        Shader s = Shaders.IRRADIANCE.getShader().use();
        s.setTexture("environmentMap", cubemap, 0);

        renderInvertedCube(irradiance, s);

        old.use();
        return irradiance;
    }

    public static CubeMap generatePrefilterMap(CubeMap cubemap) {
        CubeMap prefilter = generateEmptyMap(1024, 1024, false, true);

        Shader old = Shader.activeShader;
        Shader s = Shaders.PREFILTER.getShader().use();
        s.setTexture("environmentMap", cubemap, 0);
        s.setMat4("projection", CAPTURE_PROJECTION);

        Framebuffer oldFB = Framebuffer.activeFramebuffer;
        glBindFramebuffer(GL_FRAMEBUFFER, captureFBO);
        int maxMipLevels = 8;
        for (int mip = 0; mip < maxMipLevels; mip++) {
            int mipSize = 1024 >> mip;
            glViewport(0, 0, mipSize, mipSize);

            float roughness = (float) mip / (maxMipLevels - 1f);
            s.setFloat("roughness", roughness);

            for (CubeMap.Face face : CubeMap.Face.values()) {
                s.setMat4("view", face.viewMatrix);
                glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, face.GLTarget, prefilter.getID(), mip);
                glClear(GL_COLOR_BUFFER_BIT);
                SimpleGeometry.INV_CUBE.render();
            }
        }

        old.use();
        oldFB.use();
        return prefilter;
    }

    public static Texture generateLUTMap(int width, int height) {
        //generate the texture
        int id = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, id);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RG16, width, height, 0, GL_RG, GL_UNSIGNED_SHORT, NULL);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        //use the capture framebuffer to render the BRDF LUT
        Framebuffer prevBuffer = Framebuffer.activeFramebuffer;
        glBindFramebuffer(GL_FRAMEBUFFER, captureFBO);

        //bind the new texture to the framebuffer
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, id, 0);

        //set and clear the viewport
        glViewport(0, 0, width, height);
        glClear(GL_COLOR_BUFFER_BIT);

        //keep the old shader and render a quad to generate the LUT
        Shader prevShader = Shader.activeShader;
        Shaders.BRDF_LUT.getShader().use();
        SimpleGeometry.QUAD.render();

        //restore the previous render state
        glBindTexture(GL_TEXTURE_2D, 0);
        prevShader.use();
        prevBuffer.use();

        return new Texture(id, width, height);
    }

    public static CubeMap generateEmptyMap(int width, int height, boolean hdr, boolean mipmap) {
        int id = glGenTextures();
        glBindTexture(GL_TEXTURE_CUBE_MAP, id);

        for (CubeMap.Face face : CubeMap.Face.values()) {
            if (hdr)
                glTexImage2D(face.GLTarget, 0, GL_RGB16F, width, height, 0, GL_RGB, GL_FLOAT, NULL);
            else
                glTexImage2D(face.GLTarget, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, NULL);
        }

        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        if (mipmap) {
            glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
            glGenerateMipmap(GL_TEXTURE_CUBE_MAP);
        }
        else {
            glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        }

        glBindTexture(GL_TEXTURE_CUBE_MAP, 0);
        return new CubeMap(id, width, height);
    }
}
