package cinnamon.gui.screens.extras;

import cinnamon.collision.ColliderChecker;
import cinnamon.collision.MeshCollider;
import cinnamon.gui.ParentedScreen;
import cinnamon.gui.Screen;
import cinnamon.model.GeometryHelper;
import cinnamon.model.Vertex;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.text.Text;
import cinnamon.utils.Alignment;
import cinnamon.utils.Colors;
import org.joml.Vector3f;

import static org.lwjgl.glfw.GLFW.*;

public class NewCollisionScreen extends ParentedScreen {

    //input
    private boolean l, r, u, d, s, mouse;
    private float mouseX = 0, mouseY = 0;

    //polygons
    private final Vector3f[] //clockwise vertices
            circle = {new Vector3f(-40, 0, 0), new Vector3f(-20, 0, 0), new Vector3f(-30, -15, 0)},
            box = {new Vector3f(70, -20, 0), new Vector3f(50, -20, 0), new Vector3f(50, 0, 0), new Vector3f(70, 0, 0)},
            pentagon = {new Vector3f(50, 0, 0), new Vector3f(70, 0, 0), new Vector3f(80, -20, 0), new Vector3f(60, -35, 0), new Vector3f(40, -20, 0)};

    //settings
    private static final float speed = 1f, altSpeed = 3f;
    private final Vector3f[]
            player = circle,
            terrain = pentagon;

    public NewCollisionScreen(Screen parentScreen) {
        super(parentScreen);
    }

    @Override
    public void tick() {
        super.tick();

        float s = this.s ? altSpeed : speed;
        float x = 0f;
        float y = 0f;

        if (l) x -= s;
        if (r) x += s;
        if (u) y -= s;
        if (d) y += s;

        if (x != 0 || y != 0)
            for (Vector3f vertex : player)
                vertex.add(x, y, 0);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        super.render(matrices, mouseX, mouseY, delta);

        boolean colliding = ColliderChecker.collides2D(new MeshCollider(player), new MeshCollider(terrain));
        Text.translated(colliding ? "gui.new_collision_test_screen.colliding" : "gui.new_collision_test_screen.not_colliding")
                .render(VertexConsumer.MAIN, matrices, width / 2f, 4, Alignment.TOP_CENTER);

        matrices.pushMatrix();
        matrices.translate(width / 2f, height / 2f, 0f);

        //render lines
        VertexConsumer.MAIN.consume(GeometryHelper.line(matrices, -width, 0, width, 0, 1f, Colors.DARK_GRAY.argb));
        VertexConsumer.MAIN.consume(GeometryHelper.line(matrices, 0, -height, 0, height, 1f, Colors.DARK_GRAY.argb));

        //minkowski difference
        for (Vector3f p : player)
            for (Vector3f t : terrain)
                VertexConsumer.MAIN.consume(GeometryHelper.circle(matrices, p.x - t.x, p.y - t.y, 1.5f, 1f, 12, Colors.CYAN.argb));

        //render terrain
        Vertex[] v = new Vertex[terrain.length];
        for (int i = 0; i < terrain.length; i++)
            v[i] = new Vertex().pos(terrain[i]).mul(matrices).color(Colors.YELLOW.argb);
        VertexConsumer.LINES.consume(v);

        //render player
        v = new Vertex[player.length];
        for (int i = 0; i < player.length; i++)
            v[i] = new Vertex().pos(player[i]).mul(matrices).color(colliding ? 0xFFFFFFFF : Colors.PINK.argb);
        VertexConsumer.LINES.consume(v);

        matrices.popMatrix();
    }

    @Override
    public boolean keyPress(int key, int scancode, int action, int mods) {
        boolean press = action != GLFW_RELEASE;
        switch (key) {
            case GLFW_KEY_LEFT -> l = press;
            case GLFW_KEY_RIGHT -> r = press;
            case GLFW_KEY_UP -> u = press;
            case GLFW_KEY_DOWN -> d = press;
            case GLFW_KEY_LEFT_SHIFT -> s = press;
            default -> {
                return super.keyPress(key, scancode, action, mods);
            }
        }
        return true;
    }

    @Override
    public boolean mousePress(int button, int action, int mods) {
        boolean sup = super.mousePress(button, action, mods);
        if (sup) return true;
        
        mouse = button == GLFW_MOUSE_BUTTON_1 && action == GLFW_PRESS;

        if (mouse) {
            mouseX = client.window.mouseX;
            mouseY = client.window.mouseY;
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseMove(int x, int y) {
        if (!mouse)
            return super.mouseMove(x, y);

        float dx = x - mouseX;
        float dy = y - mouseY;

        for (Vector3f vertex : terrain)
            vertex.add(dx, dy, 0);

        mouseX = x;
        mouseY = y;
        return true;
    }
}
