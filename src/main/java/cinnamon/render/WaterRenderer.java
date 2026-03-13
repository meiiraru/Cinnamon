package cinnamon.render;

import cinnamon.math.Rotation;
import cinnamon.math.noise.PerlinNoise2D;
import cinnamon.model.StaticGeometry;
import cinnamon.render.shader.Shader;
import cinnamon.render.shader.Shaders;
import org.joml.Vector3f;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;
import static org.lwjgl.opengl.GL33.GL_TEXTURE_SWIZZLE_RGBA;

public class WaterRenderer {

    private static final int noiseTexture;

    static {
        int width = 512;
        int height = 512;
        long seed = System.nanoTime();
        int cells = 64;

        //generate noise
        PerlinNoise2D noise = new PerlinNoise2D(width, height, seed, cells);

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

        //swizzle red channel to all rgb channels
        int[] swizzleMask = {GL_RED, GL_RED, GL_RED, GL_ALPHA};
        glTexParameteriv(GL_TEXTURE_2D, GL_TEXTURE_SWIZZLE_RGBA, swizzleMask);

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
        StaticGeometry.QUAD.render();
        matrices.popMatrix();
    }

    public static int getNoiseTexture() {
        return noiseTexture;
    }
}
