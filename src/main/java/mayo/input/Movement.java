package mayo.input;

import mayo.world.entity.Entity;
import org.joml.Vector2f;
import org.joml.Vector3f;

import static org.lwjgl.glfw.GLFW.*;

public class Movement {

    private static final float MOVE_SPEED = 0.15f; //units per tick
    private static final float SPRINT_MULTIPLIER = 3f;
    private static final float PRECISION_MULTIPLIER = 0.1f;
    private static final float ROTATION_SPEED = 0.27f;

    //pos
    private final Vector3f movement = new Vector3f();
    private boolean up, down, left, right, forward, backward, sprint, slow;

    //rot
    private final Vector2f rotation = new Vector2f();
    private double mouseX, mouseY, offsetX, offsetY;
    public boolean firstMouse = true;

    public void keyPress(int key, int action) {
        boolean pressed = action != GLFW_RELEASE;
        switch (key) {
            //movement
            case GLFW_KEY_W -> forward = pressed;
            case GLFW_KEY_A -> left = pressed;
            case GLFW_KEY_S -> backward = pressed;
            case GLFW_KEY_D -> right = pressed;
            case GLFW_KEY_SPACE -> up = pressed;
            case GLFW_KEY_LEFT_SHIFT -> down = pressed;
            case GLFW_KEY_TAB -> sprint = pressed;
            case GLFW_KEY_LEFT_CONTROL -> slow = pressed;
        }
    }

    public void mouseMove(double x, double y) {
        if (firstMouse) {
            mouseX = x;
            mouseY = y;
            firstMouse = false;
        }

        offsetX += x - mouseX;
        offsetY += y - mouseY;
        mouseX = x;
        mouseY = y;

        double sensi = ROTATION_SPEED * 0.6f + 0.2f;
        double spd = sensi * sensi * sensi * 8;
        double dx = offsetX * spd;
        double dy = offsetY * spd;

        rotation.add((float) dx * 0.15f, (float) dy * 0.15f);
        rotation.y = Math.max(Math.min(rotation.y, 90), -90);

        offsetX = 0;
        offsetY = 0;
    }

    public void apply(Entity entity) {
        if (up) movement.y += 1;
        if (down) movement.y -= 1;
        if (left) movement.x -= 1;
        if (right) movement.x += 1;
        if (forward) movement.z -= 1;
        if (backward) movement.z += 1;

        float speed = MOVE_SPEED;
        if (sprint) speed *= SPRINT_MULTIPLIER;
        if (slow) speed *= PRECISION_MULTIPLIER;

        movement.mul(speed);
        entity.move(movement.x, movement.y, movement.z);
        movement.set(0);

        entity.rotate(rotation.x, rotation.y);
    }
}
