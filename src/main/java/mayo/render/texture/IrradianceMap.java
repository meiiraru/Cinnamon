package mayo.render.texture;

import mayo.model.ModelManager;
import mayo.render.Model;
import mayo.render.shader.Shader;
import mayo.render.shader.Shaders;
import mayo.utils.Resource;
import org.joml.Matrix4f;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL12.GL_TEXTURE_WRAP_R;
import static org.lwjgl.opengl.GL13.GL_TEXTURE_CUBE_MAP;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class IrradianceMap {

    private static final int captureFBO, captureRBO;
    private static final Matrix4f CAPTURE_PROJECTION = new Matrix4f().perspective((float) Math.toRadians(90f), 1f, 0.1f, 10f);
    private static final Model MODEL = ModelManager.load(new Resource("models/skybox/skybox.obj"));

    static {
        captureFBO = glGenFramebuffers();
        captureRBO = glGenRenderbuffers();

        glBindFramebuffer(GL_FRAMEBUFFER, captureFBO);
        glBindRenderbuffer(GL_RENDERBUFFER, captureRBO);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT24, 32, 32);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, captureRBO);

        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    public static CubeMap generateIrradianceMap(CubeMap cubemap) {
        int id = glGenTextures();
        glBindTexture(GL_TEXTURE_CUBE_MAP, id);

        for (CubeMap.Face face : CubeMap.Face.values())
            glTexImage2D(face.GLTarget, 0, GL_RGB, 32, 32, 0, GL_RGB, GL_UNSIGNED_BYTE, NULL);

        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        glBindFramebuffer(GL_FRAMEBUFFER, captureFBO);

        Shader s = Shaders.IRRADIANCE.getShader();
        s.use();
        s.setInt("environmentMap", 0);
        s.setMat4("projection", CAPTURE_PROJECTION);

        glActiveTexture(GL_TEXTURE0);
        cubemap.bind();

        glViewport(0, 0, 32, 32);
        for (CubeMap.Face face : CubeMap.Face.values()) {
            s.setMat4("view", face.viewMatrix);
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, face.GLTarget, id, 0);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            MODEL.renderWithoutMaterial();
        }

        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        return new CubeMap(id);
    }
}
