package cinnamon.render;

import cinnamon.Client;
import cinnamon.model.StaticGeometry;
import cinnamon.model.Vertex;
import cinnamon.render.shader.Attributes;
import cinnamon.render.shader.Shader;
import cinnamon.render.shader.Shaders;
import cinnamon.render.texture.Texture;
import cinnamon.utils.Resource;
import cinnamon.world.world.WorldClient;
import org.joml.Vector3f;

import static org.lwjgl.opengl.GL11.*;

public class CloudRenderer {

    public static final float PLANE_RADIUS = 1024f;
    public static final float CLOUD_THICKNESS = 64f;
    public static final int LAYERS = 32;

    private static final StaticGeometry cloudMesh;

    static {
        Vertex[][] faces = new Vertex[LAYERS][4];

        for (int i = 0; i < LAYERS; i++) {
            float t = (float) i / (LAYERS - 1);
            float y = t * CLOUD_THICKNESS;

            faces[i][0] = new Vertex().pos(-PLANE_RADIUS, y, -PLANE_RADIUS).uv(0, 0).normal(0, 1, 0);
            faces[i][1] = new Vertex().pos(-PLANE_RADIUS, y, PLANE_RADIUS).uv(0, 1).normal(0, 1, 0);
            faces[i][2] = new Vertex().pos(PLANE_RADIUS, y, PLANE_RADIUS).uv(1, 1).normal(0, 1, 0);
            faces[i][3] = new Vertex().pos(PLANE_RADIUS, y, -PLANE_RADIUS).uv(1, 0).normal(0, 1, 0);
        }

        cloudMesh = StaticGeometry.of(faces, Attributes.POS, Attributes.UV, Attributes.NORMAL);
    }

    public static void renderClouds(Camera camera, float deltaTime, int cloudsColor, float y, float coverage, float cloudScale) {
        /*
        glDisable(GL_CULL_FACE);
        glDepthMask(false);

        Shader s = Shaders.CLOUDS.getShader().use();
        s.setup(camera);
        s.setVec3("camPos", camera.getPosition());

        s.setFloat("time", deltaTime * 0.1f);
        s.setColor("cloudsColor", cloudsColor);
        s.setFloat("coverage", coverage);
        s.setFloat("cloudScale", cloudScale * 0.025f);
        s.setFloat("planeRadius", PLANE_RADIUS);
        s.setFloat("cloudBase", y);
        s.setFloat("cloudThickness", CLOUD_THICKNESS);

        cloudMesh.render();

        glDepthMask(true);
        glEnable(GL_CULL_FACE);
         */

        Vector3f camPos = camera.getPos();
        WorldClient world = Client.getInstance().world;

        Shader s = Shaders.CLOUDS.getShader().use();
        // ensure the shader has both forward and inverse camera matrices so it can reconstruct rays and also compute clip depth
        s.setup(camera);
        s.setupInverse(camera);

        s.setVec2("resolution", WorldRenderer.outputBuffer.getWidth(), WorldRenderer.outputBuffer.getHeight());
        s.setVec3("camPos", camPos);
        s.setColor("sunColor", world.getSky().sunColor);
        s.setVec3("sunDir", world.getSky().getSunDirection());
        s.setColor("cloudsColor", world.getSky().cloudsColor);
        s.setFloat("time", deltaTime * 0.001f);
        s.setTexture("noiseTex", Texture.of(new Resource("", "D:/downloads/noise2.png"), Texture.TextureParams.SMOOTH_SAMPLING), 0);
        s.setTexture("blueNoiseTex", Texture.of(new Resource("", "D:/downloads/blue-noise.png"), Texture.TextureParams.SMOOTH_SAMPLING), 1);
        // read scene depth from the output buffer (it contains the blitted PBR depth at this stage)
        s.setTexture("gDepth", WorldRenderer.outputBuffer.getDepthBuffer(), 2);

        s.setFloat("noiseScale", 0.1f);  // higher = smaller blobs, lower = bigger blobs
        s.setFloat("cloudScale", 32f);       // 20x bigger than the base SDF

        s.setVec3("cloudPos", camPos.x, 16, camPos.z);  // world-space center of the cloud volume
        s.setFloat("cloudCoverage", 0.85f); // 0 = clear, 1 = overcast

        s.setInt("MAX_STEPS", 72);
        s.setInt("MAX_STEPS_LIGHTS", 5);
        s.setFloat("MARCH_SIZE", 0.3f);
        s.setFloat("ABSORPTION_COEFFICIENT", 0.9f);
        s.setFloat("SCATTERING_ANISO", 0.3f);

        // Render a fullscreen quad without the fixed-function depth test (we need the fragment shader to decide and write gl_FragDepth),
        // but allow depth writes so the shader can update the depth buffer where clouds are closer than scene geometry.
        StaticGeometry.QUAD.render();

        Texture.unbindAll(3);
    }
}
