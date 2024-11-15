package cinnamon.input;

import cinnamon.Client;
import cinnamon.settings.Settings;
import cinnamon.world.entity.Entity;
import cinnamon.world.entity.living.Player;
import org.joml.Vector2f;
import org.joml.Vector3f;

import static org.lwjgl.glfw.GLFW.*;

public class Movement {

    private static final int TICKS_TO_FLY = (int) (0.3f * Client.TPS);

    //pos
    private final Vector3f movement = new Vector3f();
    private boolean up, down, left, right, forward, backward, sprint, sneak;

    //rot
    private final Vector2f rotation = new Vector2f();
    private double mouseX, mouseY, offsetX, offsetY;
    private boolean firstMouse = true;

    //flying
    private int flyTicks = 0;
    private boolean flyingToggle = false;

    public void keyPress(int key, int action) {
        boolean pressed = action != GLFW_RELEASE;
        switch (key) {
            //movement
            case GLFW_KEY_W -> forward = pressed;
            case GLFW_KEY_A -> left = pressed;
            case GLFW_KEY_S -> backward = pressed;
            case GLFW_KEY_D -> right = pressed;
            case GLFW_KEY_SPACE -> {
                up = pressed;
                if (action == GLFW_PRESS) {
                    if (flyTicks > 0) {
                        flyingToggle = true;
                        flyTicks = 0;
                    } else {
                        flyTicks = TICKS_TO_FLY;
                    }
                }
            }
            case GLFW_KEY_LEFT_SHIFT -> down = pressed;
            case GLFW_KEY_TAB -> sprint = pressed;
            case GLFW_KEY_LEFT_CONTROL -> sneak = pressed;
        }
    }

    public void mouseMove(double x, double y) {
        Settings settings = Client.getInstance().settings;

        if (firstMouse) {
            mouseX = x;
            mouseY = y;
            firstMouse = false;
        }

        offsetX += (x - mouseX) * (settings.invertX.get() ? -1 : 1);
        offsetY += (y - mouseY) * (settings.invertY.get() ? -1 : 1);
        mouseX = x;
        mouseY = y;

        double sensi = settings.sensibility.get() * 0.6f + 0.2f;
        double spd = sensi * sensi * sensi * 8;
        double dx = offsetX * spd;
        double dy = offsetY * spd;

        rotation.add((float) dx * 0.15f, (float) dy * 0.15f);

        offsetX = 0;
        offsetY = 0;
    }

    public void tick(Entity target) {
        if (flyTicks > 0)
            flyTicks--;

        if (up) movement.y += 1;
        if (down) movement.y -= 1;
        if (left) movement.x -= 1;
        if (right) movement.x += 1;
        if (forward) movement.z += 1;
        if (backward) movement.z -= 1;

        if (movement.lengthSquared() > 0) {
            target.move(movement.x, movement.y, movement.z);
            movement.set(0);
        }

        if (rotation.lengthSquared() > 0) {
            target.rotate(rotation.y, rotation.x);
            rotation.set(0);
        }

        if (target instanceof Player p) {
            boolean flying = p.isFlying();
            if (flyingToggle) {
                flyingToggle = false;
                flying = !flying;
            }

            p.updateMovementFlags(sneak, sprint, flying);
        }
    }

    public void reset() {
        this.firstMouse = true;
        this.movement.set(0);
        up = down = left = right = forward = backward = sprint = sneak = flyingToggle = false;
        flyTicks = 0;
    }
}
