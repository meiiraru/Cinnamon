package cinnamon.input;

import org.lwjgl.glfw.GLFWGamepadState;

import static cinnamon.Client.LOGGER;
import static org.lwjgl.glfw.GLFW.*;

public class JoystickManager {

    static final GLFWGamepadState gamepadState = GLFWGamepadState.create();
    private static final Joystick[] joysticks = new Joystick[GLFW_JOYSTICK_LAST + 1];
    private static int joystickCount;

    static {
        //check for connected joysticks
        for (int jid = 0; jid <= GLFW_JOYSTICK_LAST; jid++) {
            if (glfwJoystickPresent(jid))
                initializeJoystick(jid);
        }
    }

    public static void close() {
        gamepadState.free();
    }

    public static void poll() {
        if (joystickCount <= 0)
            return;

        //poll controls
        for (Joystick joystick : joysticks) {
            if (joystick != null && !joystick.poll())
                //disconnect if not present anymore
                disconnectJoystick(joystick.getJoystickID());
        }
    }

    public static void joystickConnectEvent(int jid, int event) {
        if (event == GLFW_CONNECTED) {
            initializeJoystick(jid);
        } else if (event == GLFW_DISCONNECTED) {
            disconnectJoystick(jid);
        }
    }

    private static void initializeJoystick(int jid) {
        boolean isGamepad = glfwJoystickIsGamepad(jid);
        Joystick j = new Joystick(jid, isGamepad ? glfwGetGamepadName(jid) : glfwGetJoystickName(jid), isGamepad);
        LOGGER.info("Joystick connected: %s (ID %s) %s", j.getName(), j.getJoystickID(), j.isGamepad() ? "(Gamepad)" : "(Joystick)");
        joysticks[jid] = j;
        joystickCount++;
    }

    private static void disconnectJoystick(int jid) {
        Joystick j = joysticks[jid];
        if (j == null)
            return;

        LOGGER.info("Joystick disconnected: %s (ID %s)", j.getName(), j.getJoystickID());
        joysticks[jid] = null;
        joystickCount--;
    }

    public static Joystick getJoystick(int jid) {
        return jid < 0 || jid > joysticks.length ? null : joysticks[jid];
    }

    public static int getJoystickCount() {
        return joystickCount;
    }
}