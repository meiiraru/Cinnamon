package cinnamon.input;

import cinnamon.settings.Settings;
import cinnamon.vr.XrManager;
import cinnamon.world.entity.Entity;
import cinnamon.world.entity.living.Player;
import org.joml.Vector2f;
import org.joml.Vector3f;

public class Movement {

    //pos
    protected final Vector3f movement = new Vector3f();
    protected boolean sprint, sneak, jump;

    //rot
    protected final Vector2f rotation = new Vector2f();
    protected double mouseX, mouseY, offsetX, offsetY;
    protected boolean firstMouse = true;

    //flying
    protected int flyTicks = 0;
    protected boolean flyingToggle = false;

    //xr
    protected final Vector3f xrMovement = new Vector3f();
    protected float xrRot, snapRot;

    public void tick(Entity target) {
        if (flyTicks > 0)
            flyTicks--;

        if (XrManager.isInXR()) {
            movement.add(xrMovement);
            rotation.x += xrRot + snapRot;
            snapRot = 0;
        } else {
            if (!jump && Settings.jump.get().click())
                attemptToFly();

            jump = Settings.jump.get().isPressed();
            sneak = Settings.sneak.get().isPressed();

            if (Settings.left.get().isPressed()) movement.x -= 1;
            if (Settings.right.get().isPressed()) movement.x += 1;
            if (Settings.forward.get().isPressed()) movement.z += 1;
            if (Settings.backward.get().isPressed()) movement.z -= 1;

            movement.y = jump ? 1 : sneak ? -1 : 0;

            sprint = !sneak && movement.z > 0 && (sprint || Settings.sprint.get().isPressed());
        }

        if (target instanceof Player p) {
            boolean flying = p.isFlying();
            if (flyingToggle) {
                flyingToggle = false;
                flying = !flying;
            }

            p.updateMovementFlags(sneak, sprint, flying);
        }

        if (movement.lengthSquared() > 0) {
            target.impulse(movement.x, movement.y, movement.z);
            movement.set(0);
        }

        if (rotation.lengthSquared() > 0) {
            target.rotate(rotation.y, rotation.x);
            rotation.set(0);
        }
    }

    public void reset() {
        this.firstMouse = true;
        this.movement.set(0);
        this.xrMovement.set(0);
        this.rotation.set(0);
        sprint = sneak = jump = false;
        flyTicks = 0;
    }

    private void attemptToFly() {
        if (flyTicks > 0) {
            flyingToggle = true;
            flyTicks = 0;
        } else {
            flyTicks = Settings.flyingToggleTime.get();
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

    public void xrButtonPress(int button, boolean pressed, int hand) {
        switch (button) {
            case 0 -> {
                if (!jump && pressed)
                    attemptToFly();
                jump = pressed;
            }
            case 2 -> {
                if (!pressed)
                    return;

                if (hand == 0)
                    sprint = !sprint;
                if (hand == 1) {
                    sneak = !sneak;
                }
            }
        }
        xrMovement.y = jump ? 1 : sneak ? -1 : 0;
    }

    public void xrJoystickMove(float x, float y, int hand, float lastX, float lastY) {
        float f = 0.5f; //dead zone
        int dx = x >= f ? 1 : x <= -f ? -1 : 0;
        int dy = y >= f ? 1 : y <= -f ? -1 : 0;

        //movement
        if (hand == 0)
            xrMovement.set(dx, xrMovement.y, dy);
        //rotation
        else if (hand == 1) {
            if (!Settings.xrSnapTurn.get()) {
                xrRot = dx * Settings.xrTurningAngle.get();
            } else {
                float f2 = 0.7f;
                int dxx = lastX < f2 && x >= f2 ? 1 : lastX > -f2 && x <= -f2 ? -1 : 0;
                if (dxx != 0) snapRot = dxx * Settings.xrSnapTurningAngle.get();
            }
        }
    }
}
