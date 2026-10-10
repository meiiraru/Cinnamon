package cinnamon.render;

import cinnamon.math.noise.FBMNoise;
import cinnamon.math.noise.VoronoiNoise2D;
import cinnamon.render.shader.CoreShaders;
import cinnamon.render.shader.Shader;
import cinnamon.render.texture.NoiseTexture;

public class FireRenderer {

    private static final NoiseTexture noiseTexture;

    static {
        int width = 512;
        int height = 512;
        long seed = System.nanoTime();

        //generate noise
        VoronoiNoise2D baseNoise = new VoronoiNoise2D(width, height, seed);
        FBMNoise fbmNoise = new FBMNoise(baseNoise, 3);

        //create texture
        noiseTexture = new NoiseTexture(fbmNoise);

        //free resources
        fbmNoise.free();
        baseNoise.free();
    }

    public static void initFireRenderer(Camera camera, float time) {
        //setup shader
        Shader s = CoreShaders.FIRE.getShader().use();
        s.setup(camera);
        s.setFloat("time", time * 0.03f);
    }

    public static int bindRenderer() {
        Shader s = CoreShaders.FIRE.getShader().use();
        s.setTexture("noiseTex", noiseTexture, 0);

        //return number of textures used
        return 1;
    }

    public static NoiseTexture getNoiseTexture() {
        return noiseTexture;
    }
}
