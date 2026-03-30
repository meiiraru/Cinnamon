package cinnamon.gui.screens.extras;

import cinnamon.Client;
import cinnamon.gui.ParentedScreen;
import cinnamon.gui.Screen;
import cinnamon.math.collision.*;
import cinnamon.model.GeometryHelper;
import cinnamon.render.DebugRenderer;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import org.joml.Vector3f;

import static cinnamon.world.world.CollisionWorld.renderShape;
import static org.lwjgl.glfw.GLFW.*;

public class CollisionScreen extends ParentedScreen {

    private static final float shapeRadius = 10f;
    private static final float speed = 3f;
    private static final float rotationSpeed = 5f;

    private final Screen parentScreen;
    private final CollisionShape<?> player;
    private final CollisionShape<?>[] obstacles = new CollisionShape[4 * 3];
    private final Vector3f rayPos, velocity;

    private boolean l, r, u, d, rl, rr;

    public CollisionScreen(Screen parentScreen) {
        super(parentScreen);
        this.parentScreen = parentScreen;
        this.rayPos = new Vector3f(25f, 25f, 0f);
        this.velocity = new Vector3f();

        //create player shape at starting position
        //this.player = new AABB(50 - shapeRadius, 50 - shapeRadius, -shapeRadius, 50 + shapeRadius, 50 + shapeRadius, shapeRadius);
        //this.player = new Sphere(50, 50, 0, shapeRadius);
        this.player = new OBB(50, 50, 0, shapeRadius, shapeRadius, shapeRadius).rotateZ(45f);

        //create obstacles
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 4; j++) {
                float x = 100 + (j * 50);
                float y = 50 + (i * 50);
                obstacles[i * 4 + j] = switch (i) {
                    case 0 -> new AABB(x - shapeRadius, y - shapeRadius, -shapeRadius, x + shapeRadius, y + shapeRadius, shapeRadius);
                    case 1 -> new OBB(x, y, 0, shapeRadius, shapeRadius, shapeRadius).rotateZ((j + 1) * 12.5f);
                    case 2 -> new Sphere(x, y, 0, shapeRadius);
                    default -> throw new IllegalStateException();
                };
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        tickInput();
        tickCollisions();
    }

    private void tickInput() {
        //movement
        if (l) velocity.x -= speed;
        if (r) velocity.x += speed;
        if (u) velocity.y -= speed;
        if (d) velocity.y += speed;

        //obb rotation
        if (player instanceof OBB obb) {
            if (rl) obb.rotateZ(-rotationSpeed);
            if (rr) obb.rotateZ(rotationSpeed);
        }
    }

    private void tickCollisions() {
        //apply translation to player
        player.translate(velocity);
        velocity.set(0f);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        super.render(matrices, mouseX, mouseY, delta);
        shapeVsShape(matrices);
        rayVsShape(matrices, mouseX, mouseY);
        renderShape(matrices, player, 0xFFFF72AD);
    }

    private void rayVsShape(MatrixStack matrices, int mouseX, int mouseY) {
        //length
        Vector3f len = new Vector3f(mouseX - rayPos.x, mouseY - rayPos.y, 0);

        //collision
        Ray ray = new Ray(rayPos.x, rayPos.y, 0f, len.x, len.y, 0f, len.length());
        Hit hit = player.collideRay(ray);
        boolean collided = hit != null;

        //draw line
        VertexConsumer.MAIN.consume(GeometryHelper.line(matrices, rayPos.x, rayPos.y, mouseX, mouseY, 1, collided ? 0xFFFFFF00 : 0xFFFFFFFF));

        if (collided) {
            Vector3f near = hit.position();
            DebugRenderer.renderPoint(matrices, near, 3, 0xFF72ADFF);

            Vector3f far = ray.getOrigin().fma(hit.tFar(), ray.getDirection(), new Vector3f());
            DebugRenderer.renderPoint(matrices, far, 3, 0xFFFF7200);

            Vector3f normal = hit.normal();
            VertexConsumer.MAIN.consume(GeometryHelper.line(matrices, near.x, near.y, near.x + normal.x * 10, near.y + normal.y * 10, 1, 0xFF72FF72));
        }
    }

    private void shapeVsShape(MatrixStack matrices) {
        //draw all obstacles
        for (CollisionShape<?> shape : obstacles) {
            renderShape(matrices, shape, player.intersects(shape) ? 0xFFFFFF00 : 0xFF72ADFF);
            DebugRenderer.renderPoint(matrices, shape.closestPoint(player.getCenter()), 3, 0xFF72ADFF);
        }
    }

    @Override
    public boolean mousePress(int button, int action, int mods) {
        boolean child = super.mousePress(button, action, mods);
        if (child) return true;

        if (action == GLFW_PRESS && button == GLFW_MOUSE_BUTTON_1) {
            int mouseX = Client.getInstance().window.mouseX;
            int mouseY = Client.getInstance().window.mouseY;
            rayPos.set(mouseX, mouseY, 0);
        }

        return false;
    }

    @Override
    public boolean keyPress(int key, int scancode, int action, int mods) {
        boolean press = action != GLFW_RELEASE;
        switch (key) {
            case GLFW_KEY_A, GLFW_KEY_LEFT  -> l = press;
            case GLFW_KEY_D, GLFW_KEY_RIGHT -> r = press;
            case GLFW_KEY_W, GLFW_KEY_UP    -> u = press;
            case GLFW_KEY_S, GLFW_KEY_DOWN  -> d = press;
            case GLFW_KEY_Q, GLFW_KEY_PAGE_UP   -> rl = press;
            case GLFW_KEY_E, GLFW_KEY_PAGE_DOWN -> rr = press;

            case GLFW_KEY_R -> client.setScreen(new CollisionScreen(parentScreen));

            default -> {return super.keyPress(key, scancode, action, mods);}
        }
        return true;
    }
}
