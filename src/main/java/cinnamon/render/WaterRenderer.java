package cinnamon.render;

import cinnamon.math.Rotation;
import cinnamon.math.noise.FBMNoise;
import cinnamon.math.noise.PerlinNoise2D;
import cinnamon.model.StaticGeometry;
import cinnamon.render.shader.Shader;
import cinnamon.render.shader.Shaders;
import cinnamon.render.texture.NoiseTexture;
import org.joml.Vector3f;

public class WaterRenderer {

    private static final NoiseTexture noiseTexture;

    static {
        int width = 512;
        int height = 512;
        long seed = System.nanoTime();
        int cells = 64;

        //generate noise
        PerlinNoise2D baseNoise = new PerlinNoise2D(width, height, seed, cells);
        FBMNoise fbmNoise = new FBMNoise(baseNoise, 5, 2f, 0.5f, 0.5f, 1f);

        //create texture
        noiseTexture = new NoiseTexture(fbmNoise);

        //free resources
        fbmNoise.free();
        baseNoise.free();
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

    public static void renderWaterPlane(Camera camera, MatrixStack matrices, float y, float size) {
        matrices.pushMatrix();
        Vector3f camPos = camera.getPosition();
        matrices.translate(camPos.x, y, camPos.z);

        matrices.rotate(Rotation.X.rotationDeg(-90f));
        matrices.scale(size);

        Shader.activeShader.applyMatrixStack(matrices);
        StaticGeometry.QUAD.render();
        matrices.popMatrix();
    }

    public static NoiseTexture getNoiseTexture() {
        return noiseTexture;
    }
}
