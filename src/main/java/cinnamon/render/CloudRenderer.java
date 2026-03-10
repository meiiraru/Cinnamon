package cinnamon.render;

import cinnamon.model.StaticGeometry;
import cinnamon.render.framebuffer.Framebuffer;
import cinnamon.render.shader.Shader;
import cinnamon.render.shader.Shaders;
import cinnamon.render.texture.Texture;
import cinnamon.utils.Resource;
import cinnamon.world.sky.Sky;
import org.joml.Vector3f;

import static org.lwjgl.opengl.GL11.*;

public class CloudRenderer {

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
        s.setFloat("time", deltaTime * 0.001f);
        s.setTexture("noiseTex", Texture.of(new Resource("", "D:/downloads/noise2.png"), Texture.TextureParams.SMOOTH_SAMPLING), 0);
        s.setTexture("blueNoiseTex", Texture.of(new Resource("", "D:/downloads/blue-noise.png"), Texture.TextureParams.SMOOTH_SAMPLING), 1);

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
}
