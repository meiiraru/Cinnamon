package cinnamon.render;

import cinnamon.utils.Pair;
import cinnamon.utils.Resource;
import cinnamon.utils.TextureIO;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.glfw.GLFWVidMode;

import static cinnamon.Client.LOGGER;
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
    public float guiScale = 1f, maxGuiScale = 1f;

    //mouse properties
    public int mouseX, mouseY;
    public boolean mouse1Press, mouse2Press, mouse3Press;
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

    public boolean isMouseLocked() {
        return mouseLocked;
    }

    public void toggleFullScreen() {
        fullscreen = !fullscreen;

        //set fullscreen
        if (fullscreen) {
            this.windowedX = this.x;
            this.windowedY = this.y;
            this.windowedWidth = this.width;
            this.windowedHeight = this.height;

            Pair<Long, GLFWVidMode> monitor = getMonitorProperties();
            GLFWVidMode vidMode = monitor.second();

            this.x = this.y = 0;
            this.width = vidMode.width();
            this.height = vidMode.height();

            glfwSetWindowMonitor(window, monitor.first(), x, y, width, height, vidMode.refreshRate());
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

    private Pair<Long, GLFWVidMode> getMonitorProperties() {
        //grab monitor
        long monitor = glfwGetWindowMonitor(window);
        if (monitor == NULL)
            monitor = glfwGetPrimaryMonitor();

        return Pair.of(monitor, glfwGetVideoMode(monitor));
    }

    public void setTitle(String title) {
        glfwSetWindowTitle(window, title);
    }

    public void mousePress(int button, int action, int mods) {
        boolean press = action == GLFW_PRESS;
        switch (button) {
            case GLFW_MOUSE_BUTTON_1 -> mouse1Press = press;
            case GLFW_MOUSE_BUTTON_2 -> mouse2Press = press;
            case GLFW_MOUSE_BUTTON_3 -> mouse3Press = press;
        }
    }

    public void mouseMove(double x, double y) {
        this.mouseX = (int) (x / guiScale);
        this.mouseY = (int) (y / guiScale);
    }

    public void windowMove(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void windowResize(int width, int height, float guiScale) {
        this.width = width;
        this.height = height;

        int w = this.width / 320;
        int h = this.height / 240;
        float scale = Math.min(w, h);
        this.maxGuiScale = scale;

        if (guiScale > 0)
            scale = Math.min(scale, guiScale);

        this.guiScale = Math.max(scale, 1f);
        this.scaledWidth = (int) (this.width / this.guiScale);
        this.scaledHeight = (int) (this.height / this.guiScale);
    }

    public void setIcon(Resource resource) {
        try (
                GLFWImage icon = GLFWImage.malloc();
                GLFWImage.Buffer imageBuffer = GLFWImage.malloc(1);
                TextureIO.ImageData data = TextureIO.load(resource)
        ) {
            icon.set(data.width, data.height, data.buffer);
            imageBuffer.put(0, icon);
            glfwSetWindowIcon(window, imageBuffer);
        } catch (Exception e) {
            LOGGER.error("Failed to set window icon", e);
        }
    }
}
