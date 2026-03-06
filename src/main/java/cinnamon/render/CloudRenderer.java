package cinnamon.render;

import cinnamon.model.StaticGeometry;
import cinnamon.model.Vertex;
import cinnamon.render.shader.Attributes;
import cinnamon.render.shader.Shader;
import cinnamon.render.shader.Shaders;

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
    }
}
