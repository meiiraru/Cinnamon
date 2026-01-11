package cinnamon.render;

import cinnamon.model.SimpleGeometry;
import cinnamon.render.shader.Shader;
import cinnamon.render.shader.Shaders;
import cinnamon.utils.PerlinNoise;
import cinnamon.utils.Rotation;
import org.joml.Vector3f;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.GL_LINEAR;
import static org.lwjgl.opengl.GL11.GL_LINEAR_MIPMAP_LINEAR;
import static org.lwjgl.opengl.GL11.GL_RED;
import static org.lwjgl.opengl.GL11.GL_REPEAT;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glTexImage2D;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;

public class WaterRenderer {

    private static final int noiseTexture;

    static {
        int width = 512;
        int height = 512;
        long seed = System.nanoTime();
        int cells = 64;

        //generate noise
        PerlinNoise noise = new PerlinNoise(width, height, seed, cells);

        //create texture
        noiseTexture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, noiseTexture);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RED, width, height, 0, GL_RED, GL_UNSIGNED_BYTE, noise.getBuffer());

        //linear filtering for smooth sampling
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glGenerateMipmap(GL_TEXTURE_2D);

        //free resources
        glBindTexture(GL_TEXTURE_2D, 0);
        noise.free();
    }

    public static int prepareWaterRenderer(Camera camera, float time) {
        //setup shader
        Shader s = Shaders.WATER.getShader().use();

        s.setup(camera);
        s.setFloat("time", time * 0.0003f);
        s.setTexture("noiseTex", noiseTexture, 0);

        //return number of textures used
        return 1;
    }

    public static void renderDefaultWaterPlane(Camera camera, MatrixStack matrices, float y, float size) {
        matrices.pushMatrix();
        Vector3f camPos = camera.getPosition();
        matrices.translate(camPos.x, y, camPos.z);

        matrices.rotate(Rotation.X.rotationDeg(-90f));
        matrices.scale(size);

        Shader.activeShader.applyMatrixStack(matrices);
        SimpleGeometry.QUAD.render();
        matrices.popMatrix();
    }

    public static int getNoiseTexture() {
        return noiseTexture;
    }
}
