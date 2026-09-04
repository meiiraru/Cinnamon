package cinnamon.render;

import cinnamon.math.noise.BlueNoise2D;
import cinnamon.math.noise.WhiteNoise2D;
import cinnamon.model.StaticGeometry;
import cinnamon.render.framebuffer.Framebuffer;
import cinnamon.render.shader.PostProcess;
import cinnamon.render.shader.Shader;
import cinnamon.render.shader.Shaders;
import cinnamon.render.texture.NoiseTexture;
import cinnamon.render.texture.Texture;
import cinnamon.world.sky.Sky;
import org.joml.Vector3f;

import static org.lwjgl.opengl.GL11.GL_ALWAYS;
import static org.lwjgl.opengl.GL11.GL_LEQUAL;
import static org.lwjgl.opengl.GL11.glDepthFunc;

public class CloudRenderer {

    public final static Framebuffer
            cloudBuffer = new Framebuffer(Framebuffer.COLOR_BUFFER | Framebuffer.DEPTH_BUFFER),
            blurBuffer = new Framebuffer(Framebuffer.COLOR_BUFFER);

    private final static NoiseTexture whiteNoise, blueNoise;

    static {
        //generate noises
        long seed = System.nanoTime();
        WhiteNoise2D wNoise = new WhiteNoise2D(512, 512, seed);
        BlueNoise2D bNoise = new BlueNoise2D(512, 512, seed);

        //create white noise texture
        whiteNoise = new NoiseTexture(wNoise);
        blueNoise = new NoiseTexture(bNoise);

        //free resources
        wNoise.free();
        bNoise.free();
    }

    public static void renderClouds(Framebuffer targetBuffer, Camera camera, float deltaTime, Sky sky) {
        if (sky.cloudCoverage <= 0f)
            return;

        cloudBuffer.resizeTo(targetBuffer);
        cloudBuffer.useClear();
        cloudBuffer.adjustViewPort();

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

        s.setFloat("noiseScale", sky.cloudScale * 0.1f);
        s.setFloat("cloudScale", 32f);

        s.setVec3("cloudPos", camPos.x, sky.cloudHeight, camPos.z);
        s.setFloat("cloudCoverage", sky.cloudCoverage);

        glDepthFunc(GL_ALWAYS);
        StaticGeometry.QUAD.render();

        Texture.unbindAll(3);
        glDepthFunc(GL_LEQUAL);

        int tex = Blur.boxBlur(cloudBuffer.getColorBuffer(), cloudBuffer.getWidth(), cloudBuffer.getHeight(), 1, blurBuffer);

        //blit to target buffer
        targetBuffer.use();
        targetBuffer.adjustViewPort();

        Shader blit = PostProcess.BLIT_COLOR_DEPTH.getShader().use();
        blit.setTexture("colorTex", tex, 0);
        blit.setTexture("depthTex", cloudBuffer.getDepthBuffer(), 1);

        StaticGeometry.QUAD.render();

        Texture.unbindAll(2);
    }

    public static NoiseTexture getWhiteNoiseTexture() {
        return whiteNoise;
    }

    public static NoiseTexture getBlueNoiseTexture() {
        return blueNoise;
    }
}
