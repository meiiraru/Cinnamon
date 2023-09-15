package mayo.render;

import org.lwjgl.glfw.GLFWVidMode;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Window {

    //window address
    private final long window;

    //window properties
    public int x, y;
    public int width, height;

    //fullscreen stuff
    private int windowedX, windowedY;
    private int windowedWidth, windowedHeight;
    private boolean fullscreen;

    //gui properties
    public int scaledWidth = width, scaledHeight = height;
    public float guiScale = 3f;

    //mouse properties
    public int mouseX, mouseY;
    private boolean mouseLocked;

    public Window(long window, int width, int height) {
        this.window = window;

        //dimensions
        this.width = width;
        this.height = height;

        //grab pos
        int[] x = new int[1];
        int[] y = new int[1];
        glfwGetWindowPos(window, x, y);
        this.x = x[0];
        this.y = y[0];
    }

    public void exit() {
        glfwSetWindowShouldClose(window, true);
    }

    public boolean unlockMouse() {
        if (!mouseLocked)
            return false;

        //unlock cursor
        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);

        //move cursor to the window's center
        glfwSetCursorPos(window, width / 2f, height / 2f);

        //save state
        this.mouseLocked = false;

        return true;
    }

    public boolean lockMouse() {
        if (mouseLocked)
            return false;

        //lock cursor
        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);

        //save state
        this.mouseLocked = true;

        return true;
    }

    public void toggleFullScreen() {
        fullscreen = !fullscreen;

        //set fullscreen
        if (fullscreen) {
            this.windowedX = this.x;
            this.windowedY = this.y;
            this.windowedWidth = this.width;
            this.windowedHeight = this.height;

            GLFWVidMode vidMode = getMonitorProperties();

            this.x = this.y = 0;
            this.width = vidMode.width();
            this.height = vidMode.height();

            glfwSetWindowMonitor(window, glfwGetPrimaryMonitor(), x, y, width, height, -1);
        }
        //set windowed
        else {
            this.x = this.windowedX;
            this.y = this.windowedY;
            this.width = this.windowedWidth;
            this.height = this.windowedHeight;

            glfwSetWindowMonitor(window, NULL, x, y, width, height, -1);
        }
    }

    private GLFWVidMode getMonitorProperties() {
        //grab monitor
        long monitor = glfwGetWindowMonitor(window);
        if (monitor == NULL)
            monitor = glfwGetPrimaryMonitor();

        return glfwGetVideoMode(monitor);
    }

    public void mouseMove(double x, double y) {
        this.mouseX = (int) (x / guiScale);
        this.mouseY = (int) (y / guiScale);
    }

    public void windowMove(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void windowResize(int width, int height) {
        this.width = width;
        this.height = height;
        this.scaledWidth = (int) (width / guiScale);
        this.scaledHeight = (int) (height / guiScale);
    }
}
