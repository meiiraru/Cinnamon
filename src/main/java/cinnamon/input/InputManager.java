package cinnamon.input;

import cinnamon.Client;
import cinnamon.render.Window;

import static org.lwjgl.glfw.GLFW.*;

public class InputManager {

    private static Window getWindow() {
        return Client.getInstance().window;
    }

    private static long getWindowHandle() {
        return getWindow().getHandle();
    }

    public static void setRawMouseInput(boolean raw) {
        if (glfwRawMouseMotionSupported())
            glfwSetInputMode(getWindowHandle(), GLFW_RAW_MOUSE_MOTION, raw ? GLFW_TRUE : GLFW_FALSE);
    }

    public static boolean isMousePressed(int button) {
        return glfwGetMouseButton(getWindowHandle(), button) == GLFW_PRESS;
    }

    public static boolean isKeyPressed(int key) {
        return glfwGetKey(getWindowHandle(), key) == GLFW_PRESS;
    }

    public static boolean isModsPressed(int modMask) {
        return (getWindow().modsMask & modMask) == modMask;
    }

    public static int getMouseX() {
        return getWindow().mouseX;
    }

    public static int getMouseY() {
        return getWindow().mouseY;
    }
}
