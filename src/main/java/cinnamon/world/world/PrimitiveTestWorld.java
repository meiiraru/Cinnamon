package cinnamon.world.world;

import cinnamon.model.GeometryHelper;
import cinnamon.model.Vertex;
import cinnamon.render.Camera;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.utils.AABB;
import cinnamon.utils.Colors;

import java.util.List;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_G;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;

public class PrimitiveTestWorld extends WorldClient {

    private static final AABB floor = new AABB(-1000f, 0f, -1000f, 1000f, 0f, 1000f);

    private boolean renderNormals;

    @Override
    protected void tempLoad() {
        //no nothing
    }

    @Override
    public int renderTerrain(Camera camera, MatrixStack matrices, float delta) {
        //floor (plane)
        VertexConsumer.WORLD_MAIN.consume(GeometryHelper.plane(matrices, floor.minX(), floor.getCenter().y, floor.minZ(), floor.maxX(), floor.maxZ(), Colors.GREEN.rgba));

        //line
        render(matrices, GeometryHelper.line(matrices, -13, 2, -5, -9, 4, 5, 0.1f, Colors.YELLOW.rgba));

        //box
        render(matrices, GeometryHelper.cube(matrices, -5, 2, 5, -4, 3, 6, Colors.RED.rgba));

        //pyramid
        render(matrices, GeometryHelper.pyramid(matrices, -3, 2, -3, -1, 4, -1, Colors.CYAN.rgba));

        //cone
        render(matrices, GeometryHelper.cone(matrices, 8, 3, -6, 2f, 4, 12, Colors.PURPLE.rgba));

        //cylinder
        render(matrices, GeometryHelper.cylinder(matrices, -9, 2, -8, 1.5f, 3, 12, Colors.ORANGE.rgba));

        //sphere
        render(matrices, GeometryHelper.sphere(matrices, 5, 5, 5, 2f, 12, Colors.BLUE.rgba));

        //capsule
        render(matrices, GeometryHelper.capsule(matrices, 0, 2, -12, 0.5f, 2, 12, Colors.PINK.rgba));

        return super.renderTerrain(camera, matrices, delta);
    }

    private void render(MatrixStack matrices, Vertex[][] vertices) {
        VertexConsumer.WORLD_MAIN.consume(vertices);
        if (renderNormals)
            TransparentWorld.renderNormals(matrices, vertices);
    }

    @Override
    public List<AABB> getTerrainCollisions(AABB region) {
        List<AABB> list = super.getTerrainCollisions(region);
        list.add(floor);
        return list;
    }

    @Override
    public void keyPress(int key, int scancode, int action, int mods) {
        if (action == GLFW_PRESS && key == GLFW_KEY_G)
            renderNormals = !renderNormals;
        super.keyPress(key, scancode, action, mods);
    }
}
