package cinnamon.render;

import cinnamon.math.noise.FBMNoise;
import cinnamon.math.noise.VoronoiNoise2D;
import cinnamon.render.shader.Shader;
import cinnamon.render.shader.Shaders;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;
import static org.lwjgl.opengl.GL33.GL_TEXTURE_SWIZZLE_RGBA;

public class FireRenderer {

    private static final int noiseTexture;

    static {
        int width = 512;
        int height = 512;
        long seed = System.nanoTime();

        //generate noise
        VoronoiNoise2D baseNoise = new VoronoiNoise2D(width, height, seed);
        FBMNoise fbmNoise = new FBMNoise(baseNoise, 3);

        //create texture
        noiseTexture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, noiseTexture);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RED, width, height, 0, GL_RED, GL_UNSIGNED_BYTE, fbmNoise.getBuffer());

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
        fbmNoise.free();
        baseNoise.free();
    }

    public static int prepareFireRenderer(Camera camera, float time) {
        //setup shader
        Shader s = Shaders.FIRE.getShader().use();

        s.setup(camera);
        s.setFloat("time", time * 0.03f);
        s.setTexture("noiseTex", noiseTexture, 0);

        //return number of textures used
        return 1;
    }

    public static int getNoiseTexture() {
        return noiseTexture;
    }
}
