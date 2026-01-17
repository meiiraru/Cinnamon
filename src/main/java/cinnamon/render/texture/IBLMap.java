package cinnamon.render.texture;

import cinnamon.model.SimpleGeometry;
import cinnamon.render.framebuffer.Framebuffer;
import cinnamon.render.shader.Shader;
import cinnamon.render.shader.Shaders;
import org.joml.Math;
import org.joml.Matrix4f;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL12.GL_TEXTURE_WRAP_R;
import static org.lwjgl.opengl.GL13.GL_TEXTURE_CUBE_MAP;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class IBLMap {

    private static final int captureFBO = glGenFramebuffers();
    private static final Matrix4f CAPTURE_PROJECTION = new Matrix4f().perspective(Math.toRadians(90f), 1f, 0.1f, 10f);

    public static CubeMap hdrToCubemap(Texture texture, boolean hdr) {
        int id = generateEmptyMap(512, false);

        Shader old = Shader.activeShader;
        Shader s = Shaders.EQUIRECTANGULAR_TO_CUBEMAP.getShader().use();
        s.setTexture("equirectangularMap", texture, 0);
        s.setMat4("projection", CAPTURE_PROJECTION);
        s.setBool("hdr", hdr);

        glViewport(0, 0, 512, 512);
        renderInvertedCube(id, s);

        old.use();
        return new CubeMap(id, 512, 512);
    }

    public static CubeMap generateIrradianceMap(CubeMap cubemap) {
        int id = generateEmptyMap(32, false);

        Shader old = Shader.activeShader;
        Shader s = Shaders.IRRADIANCE.getShader().use();
        s.setTexture("environmentMap", cubemap, 0);
        s.setMat4("projection", CAPTURE_PROJECTION);

        glViewport(0, 0, 32, 32);
        renderInvertedCube(id, s);

        old.use();
        return new CubeMap(id, 32, 32);
    }

    private static void renderInvertedCube(int id, Shader s) {
        Framebuffer oldFB = Framebuffer.activeFramebuffer;
        glBindFramebuffer(GL_FRAMEBUFFER, captureFBO);
        for (CubeMap.Face face : CubeMap.Face.values()) {
            s.setMat4("view", face.viewMatrix);
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, face.GLTarget, id, 0);
            glClear(GL_COLOR_BUFFER_BIT);
            SimpleGeometry.INV_CUBE.render();
        }

        oldFB.use();
    }

    public static CubeMap generatePrefilterMap(CubeMap cubemap) {
        int id = generateEmptyMap(1024, true);

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
                glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, face.GLTarget, id, mip);
                glClear(GL_COLOR_BUFFER_BIT);
                SimpleGeometry.INV_CUBE.render();
            }
        }

        old.use();
        oldFB.use();
        return new CubeMap(id, 1024, 1024);
    }

    public static Texture brdfLUT(int size) {
        int id = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, id);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RG16F, size, size, 0, GL_RG, GL_FLOAT, NULL);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        Framebuffer oldFB = Framebuffer.activeFramebuffer;
        glBindFramebuffer(GL_FRAMEBUFFER, captureFBO);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, id, 0);

        glViewport(0, 0, size, size);
        glClear(GL_COLOR_BUFFER_BIT);

        Shader old = Shader.activeShader;
        Shaders.BRDF_LUT.getShader().use();
        SimpleGeometry.QUAD.render();

        glBindTexture(GL_TEXTURE_2D, 0);

        old.use();
        oldFB.use();
        return new Texture(id, size, size);
    }

    private static int generateEmptyMap(int size, boolean mipmap) {
        int id = glGenTextures();
        glBindTexture(GL_TEXTURE_CUBE_MAP, id);

        for (CubeMap.Face face : CubeMap.Face.values())
            glTexImage2D(face.GLTarget, 0, GL_RGB16F, size, size, 0, GL_RGB, GL_FLOAT, NULL);

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
        return id;
    }
}
