package cinnamon.input;

import cinnamon.Client;

import static org.lwjgl.glfw.GLFW.*;

public class InputManager {

    private static long getWindowHandle() {
        return Client.getInstance().window.getHandle();
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
}
