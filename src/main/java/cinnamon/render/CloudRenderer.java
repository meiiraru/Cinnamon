package cinnamon.render;

import cinnamon.math.noise.BlueNoise2D;
import cinnamon.math.noise.Noise;
import cinnamon.math.noise.WhiteNoise2D;
import cinnamon.model.StaticGeometry;
import cinnamon.render.framebuffer.Framebuffer;
import cinnamon.render.shader.Shader;
import cinnamon.render.shader.Shaders;
import cinnamon.render.texture.Texture;
import cinnamon.world.sky.Sky;
import org.joml.Vector3f;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;
import static org.lwjgl.opengl.GL33.GL_TEXTURE_SWIZZLE_RGBA;

public class CloudRenderer {

    private final static int whiteNoise, blueNoise;

    static {
        //generate noises
        long seed = System.nanoTime();
        WhiteNoise2D wNoise = new WhiteNoise2D(512, 512, seed);
        BlueNoise2D bNoise = new BlueNoise2D(512, 512, seed);

        //create white noise texture
        whiteNoise = genNoiseTexture(wNoise);
        blueNoise = genNoiseTexture(bNoise);

        //free resources
        wNoise.free();
        bNoise.free();
    }

    private static int genNoiseTexture(Noise noise) {
        int id = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, id);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RED, noise.getWidth(), noise.getHeight(), 0, GL_RED, GL_UNSIGNED_BYTE, noise.getBuffer());

        //enable mipmapping and wrapping
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glGenerateMipmap(GL_TEXTURE_2D);

        //swizzle red channel to all rgb channels
        int[] swizzleMask = {GL_RED, GL_RED, GL_RED, GL_ALPHA};
        glTexParameteriv(GL_TEXTURE_2D, GL_TEXTURE_SWIZZLE_RGBA, swizzleMask);

        glBindTexture(GL_TEXTURE_2D, 0);
        return id;
    }

    public static void renderClouds(Framebuffer targetBuffer, Camera camera, float deltaTime, Sky sky, float y, float coverage, float cloudScale) {
        Vector3f camPos = camera.getPos();

        Shader s = Shaders.CLOUDS.getShader().use();
        s.setup(camera);
        s.setupInverse(camera);

        s.setVec2("resolution", targetBuffer.getWidth(), targetBuffer.getHeight());
        s.setVec3("camPos", camPos);
        s.setColor("sunColor", sky.sunColor);
        s.setVec3("sunDir", sky.getSunDirection());
        s.setColor("cloudsColor", sky.cloudsColor);
        s.setFloat("time", deltaTime * 0.005f);
        s.setTexture("noiseTex", whiteNoise, 0);
        s.setTexture("blueNoiseTex", blueNoise, 1);

        s.setTexture("gDepth", targetBuffer.getDepthBuffer(), 2);

        s.setFloat("noiseScale", 0.1f);
        s.setFloat("cloudScale", cloudScale * 32f);

        s.setVec3("cloudPos", camPos.x, y, camPos.z);
        s.setFloat("cloudCoverage", coverage);

        s.setInt("MAX_STEPS", 72);
        s.setInt("MAX_STEPS_LIGHTS", 5);
        s.setFloat("MARCH_SIZE", 0.3f);
        s.setFloat("ABSORPTION_COEFFICIENT", 0.9f);
        s.setFloat("SCATTERING_ANISO", 0.3f);

        glDepthFunc(GL_ALWAYS);
        StaticGeometry.QUAD.render();

        Texture.unbindAll(3);
        glDepthFunc(GL_LEQUAL);
    }

    public static int getWhiteNoiseTexture() {
        return whiteNoise;
    }

    public static int getBlueNoiseTexture() {
        return blueNoise;
    }
}
