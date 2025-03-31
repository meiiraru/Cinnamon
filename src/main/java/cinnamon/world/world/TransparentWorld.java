package cinnamon.world.world;

import cinnamon.model.GeometryHelper;
import cinnamon.model.Vertex;
import cinnamon.render.Camera;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.utils.Colors;
import cinnamon.utils.Resource;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;

public class TransparentWorld extends WorldClient {

    private static final Resource IMAGE = new Resource("textures/shader/cat-cat-jumping.png");

    private final List<Vertex[][]> vertices = new ArrayList<>();
    private boolean renderNormals;

    @Override
    protected void tempLoad() {
        //super.tempLoad();
        player.updateMovementFlags(false, false, true);
        gen();
    }

    @Override
    protected void renderWorld(Camera camera, MatrixStack matrices, float delta) {
        for (Vertex[][] v : vertices)
            VertexConsumer.WORLD_MAIN.consume(v);

        if (renderNormals)
            for (Vertex[][] v : vertices)
                renderNormals(matrices, v);

        matrices.pushMatrix();
        matrices.translate(20, 20, 0);
        camera.billboard(matrices);
        Vertex[] v = GeometryHelper.quad(matrices, -0.5f, -0.5f, 1, 1);
        matrices.popMatrix();

        if (renderNormals)
            renderNormals(matrices, v);

        VertexConsumer.SCREEN_UV.consume(v, IMAGE);

        super.renderWorld(camera, matrices, delta);
    }

    @Override
    protected void renderSky(MatrixStack matrices, float delta) {
        //super.renderSky(matrices, delta);
    }

    private void gen() {
        vertices.clear();
        MatrixStack matrices = new MatrixStack();
        int r = 10;
        int r2 = r + r;

        for (int i = 0; i < 100; i++) {
            matrices.identity();
            matrices.translate((float) (Math.random() * r2) - r, (float) (Math.random() * r2) - r, (float) (Math.random() * r2) - r);
            Vertex[][] cube = GeometryHelper.cube(matrices, -0.5f, -0.5f, -0.5f, 0.5f, 0.5f, 0.5f, 0);
            //references baby!
            //n - v0 to v3
            cube[0][0].color(0xAAFFFFFF);
            cube[0][1].color(0xAA00FFFF);
            cube[0][2].color(0xAA0000FF);
            cube[0][3].color(0xAAFF00FF);
            //s - v4 to v7
            cube[2][0].color(0xAA00FF00);
            cube[2][1].color(0xAAFFFF00);
            cube[2][2].color(0xAAFF0000);
            cube[2][3].color(0xAA000000);
            vertices.add(cube);
        }
    }

    public static void renderNormals(MatrixStack matrices, Vertex[][] vertices) {
        int c = 0;
        for (Vertex[] v : vertices)
            for (Vertex vertex : v)
                renderNormal(matrices, vertex, c++);
    }

    public static void renderNormals(MatrixStack matrices, Vertex[] vertices) {
        for (int i = 0; i < vertices.length; i++)
            renderNormal(matrices, vertices[i], i);
    }

    private static void renderNormal(MatrixStack matrices, Vertex vertex, int i) {
        Vector3f p0 = vertex.getPosition();
        Vector3f p1 = vertex.getNormal().mul(0.5f, new Vector3f()).add(p0);
        VertexConsumer.WORLD_MAIN.consume(GeometryHelper.line(matrices, p0.x, p0.y, p0.z, p1.x, p1.y, p1.z, 0.05f, Colors.RAINBOW[i % Colors.RAINBOW.length].rgb + (0xFF << 24)));
    }

    @Override
    public void keyPress(int key, int scancode, int action, int mods) {
        if (action == GLFW_PRESS) {
            if (key == GLFW_KEY_F)
                gen();
            else if (key == GLFW_KEY_G)
                renderNormals = !renderNormals;
        }
        super.keyPress(key, scancode, action, mods);
    }
}
