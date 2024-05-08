package mayo.render.texture;

import mayo.model.SimpleGeometry;
import mayo.render.shader.Shader;
import mayo.render.shader.Shaders;
import org.joml.Matrix4f;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL12.GL_TEXTURE_WRAP_R;
import static org.lwjgl.opengl.GL13.GL_TEXTURE_CUBE_MAP;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class IBLMap {

    private static final int captureFBO = glGenFramebuffers();
    private static final Matrix4f CAPTURE_PROJECTION = new Matrix4f().perspective((float) Math.toRadians(90f), 1f, 0.1f, 10f);

    public static CubeMap hdrToCubemap(HDRTexture hdr) {
        int id = generateEmptyMap(512, GL_LINEAR_MIPMAP_LINEAR);

        Shader s = Shaders.EQUIRECTANGULAR_TO_CUBEMAP.getShader().use();
        s.setInt("equirectangularMap", 0);
        s.setMat4("projection", CAPTURE_PROJECTION);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, hdr.getID());

        glViewport(0, 0, 512, 512);
        glBindFramebuffer(GL_FRAMEBUFFER, captureFBO);
        for (CubeMap.Face face : CubeMap.Face.values()) {
            s.setMat4("view", face.viewMatrix);
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, face.GLTarget, id, 0);
            glClear(GL_COLOR_BUFFER_BIT);
            SimpleGeometry.INVERTED_CUBE.render();
        }

        glBindFramebuffer(GL_FRAMEBUFFER, 0);

        glBindTexture(GL_TEXTURE_CUBE_MAP, id);
        glGenerateMipmap(GL_TEXTURE_CUBE_MAP);

        return new CubeMap(id);
    }

    public static CubeMap generateIrradianceMap(CubeMap cubemap) {
        int id = generateEmptyMap(32, GL_LINEAR);

        glBindFramebuffer(GL_FRAMEBUFFER, captureFBO);

        Shader s = Shaders.IRRADIANCE.getShader().use();
        s.setInt("environmentMap", 0);
        s.setMat4("projection", CAPTURE_PROJECTION);

        glActiveTexture(GL_TEXTURE0);
        cubemap.bind();

        glViewport(0, 0, 32, 32);
        glBindFramebuffer(GL_FRAMEBUFFER, captureFBO);
        for (CubeMap.Face face : CubeMap.Face.values()) {
            s.setMat4("view", face.viewMatrix);
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, face.GLTarget, id, 0);
            glClear(GL_COLOR_BUFFER_BIT);
            SimpleGeometry.INVERTED_CUBE.render();
        }

        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        return new CubeMap(id);
    }

    public static CubeMap generatePrefilterMap(CubeMap cubemap) {
        int id = generateEmptyMap(128, GL_LINEAR_MIPMAP_LINEAR);
        glGenerateMipmap(GL_TEXTURE_CUBE_MAP);

        Shader s = Shaders.PREFILTER.getShader().use();
        s.setInt("environmentMap", 0);
        s.setMat4("projection", CAPTURE_PROJECTION);

        glActiveTexture(GL_TEXTURE0);
        cubemap.bind();

        glBindFramebuffer(GL_FRAMEBUFFER, captureFBO);
        int maxMipLevels = 5;
        for (int mip = 0; mip < maxMipLevels; mip++) {
            int mipSize = (int) (128 * Math.pow(0.5f, mip));
            glViewport(0, 0, mipSize, mipSize);

            float roughness = (float) mip / (maxMipLevels - 1f);
            s.setFloat("roughness", roughness);

            for (CubeMap.Face face : CubeMap.Face.values()) {
                s.setMat4("view", face.viewMatrix);
                glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, face.GLTarget, id, mip);
                glClear(GL_COLOR_BUFFER_BIT);
                SimpleGeometry.INVERTED_CUBE.render();
            }
        }

        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        return new CubeMap(id);
    }

    public static int brdfLUT() {
        int id = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, id);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RG16F, 512, 512, 0, GL_RG, GL_FLOAT, NULL);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        glBindFramebuffer(GL_FRAMEBUFFER, captureFBO);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, id, 0);

        glViewport(0, 0, 512, 512);
        Shaders.BRDF_LUT.getShader().use();
        glClear(GL_COLOR_BUFFER_BIT);
        SimpleGeometry.QUAD.render();

        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        return id;
    }

    private static int generateEmptyMap(int size, int minFilter) {
        int id = glGenTextures();
        glBindTexture(GL_TEXTURE_CUBE_MAP, id);

        for (CubeMap.Face face : CubeMap.Face.values())
            glTexImage2D(face.GLTarget, 0, GL_RGB16F, size, size, 0, GL_RGB, GL_FLOAT, NULL);

        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MIN_FILTER, minFilter);
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        return id;
    }
}
