package cinnamon.world.world;

import cinnamon.model.GeometryHelper;
import cinnamon.model.Vertex;
import cinnamon.render.Camera;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_F;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;

public class TransparentWorld extends WorldClient {

    List<Vertex[][]> vertices = new ArrayList<>();

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
            matrices.push();
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
            matrices.pop();
        }
    }

    @Override
    public void keyPress(int key, int scancode, int action, int mods) {
        if (action == GLFW_PRESS && key == GLFW_KEY_F)
            gen();
        super.keyPress(key, scancode, action, mods);
    }
}
