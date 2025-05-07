package cinnamon.input;

import cinnamon.Client;
import cinnamon.settings.Settings;
import cinnamon.vr.XrManager;
import cinnamon.world.entity.Entity;
import cinnamon.world.entity.living.Player;
import org.joml.Vector2f;
import org.joml.Vector3f;

import static org.lwjgl.glfw.GLFW.*;

public class Movement {

    private static final int TICKS_TO_FLY = (int) (0.3f * Client.TPS);

    //pos
    private final Vector3f movement = new Vector3f();
    private boolean sprint, sneak;

    //rot
    private final Vector2f rotation = new Vector2f();
    private double mouseX, mouseY, offsetX, offsetY;
    private boolean firstMouse = true;

    //flying
    private int flyTicks = 0;
    private boolean flyingToggle = false;

    //xr
    private final Vector3f xrMovement = new Vector3f();
    private float xrRot = 0f;

    public void keyPress(int key, int action) {
        boolean pressed = action != GLFW_RELEASE;
        switch (key) {
            case GLFW_KEY_TAB -> sprint = pressed;
            case GLFW_KEY_LEFT_CONTROL -> sneak = pressed;
        }

        if (!pressed)
            return;

        switch (key) {
            //movement
            case GLFW_KEY_W -> movement.z += 1;
            case GLFW_KEY_A -> movement.x -= 1;
            case GLFW_KEY_S -> movement.z -= 1;
            case GLFW_KEY_D -> movement.x += 1;
            case GLFW_KEY_SPACE -> {
                movement.y += 1;
                if (action == GLFW_PRESS)
                    attemptToFly();
            }
            case GLFW_KEY_LEFT_SHIFT -> movement.y -= 1;
        }
    }

    private void attemptToFly() {
        if (flyTicks > 0) {
            flyingToggle = true;
            flyTicks = 0;
        } else {
            flyTicks = TICKS_TO_FLY;
        }
    }

    public void mouseMove(double x, double y) {
        if (firstMouse) {
            mouseX = x;
            mouseY = y;
            firstMouse = false;
        }

        offsetX += (x - mouseX) * (Settings.invertX.get() ? -1 : 1);
        offsetY += (y - mouseY) * (Settings.invertY.get() ? -1 : 1);
        mouseX = x;
        mouseY = y;

        double sensi = Settings.sensibility.get() * 0.6f + 0.2f;
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

        if (XrManager.isInXR()) {
            movement.add(xrMovement);
            rotation.add(xrRot, 0);
        }

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
        this.xrMovement.set(0);
        this.rotation.set(0);
        sprint = sneak = flyingToggle = false;
        flyTicks = 0;
    }

    public void xrButtonPress(int button, boolean pressed, int hand) {
        switch (button) {
            case 0 -> {
                xrMovement.y = pressed ? 1 : 0;
                if (pressed)
                    attemptToFly();
            }
            case 2 -> {
                if (!pressed)
                    return;

                if (hand == 0)
                    sprint = !sprint;
                if (hand == 1)
                    sneak = !sneak;
            }
        }
    }

    public void xrJoystickMove(float x, float y, int hand, float lastX, float lastY) {
        float f = 0.5f; //dead zone
        int dx = x >= f ? 1 : x <= -f ? -1 : 0;
        int dy = y >= f ? 1 : y <= -f ? -1 : 0;

        //movement
        if (hand == 0)
            xrMovement.set(dx, 0, dy);
        //rotation
        else if (hand == 1)
            xrRot = dx * 3f;
    }
}
