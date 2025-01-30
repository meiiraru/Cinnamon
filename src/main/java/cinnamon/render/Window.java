package cinnamon.render;

import cinnamon.utils.Resource;
import cinnamon.utils.TextureIO;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.glfw.GLFWVidMode;

import java.util.function.BiConsumer;

import static cinnamon.Client.LOGGER;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Window {

    public static final int DEFAULT_WIDTH = 854, DEFAULT_HEIGHT = 480;

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

            long monitor = getCurrentMonitor();
            GLFWVidMode vidMode = glfwGetVideoMode(monitor);

            this.x = this.y = 0;
            this.width = vidMode.width();
            this.height = vidMode.height();

            glfwSetWindowMonitor(window, monitor, x, y, width, height, vidMode.refreshRate());
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

    public long getCurrentMonitor() {
        int overlap, bestoverlap = 0;
        long bestmonitor = glfwGetPrimaryMonitor();

        PointerBuffer monitors;
        monitors = glfwGetMonitors();

        if (monitors == null)
            return bestmonitor;

        int[] mx = {0}, my = {0};
        while (monitors.hasRemaining()) {
            long monitor = monitors.get();

            GLFWVidMode mode = glfwGetVideoMode(monitor);
            if (mode == null)
                continue;

            glfwGetMonitorPos(monitor, mx, my);
            int xOverlap = Math.max(0, Math.min(x + width,  mx[0] + mode.width())  - Math.max(x, mx[0]));
            int yOverlap = Math.max(0, Math.min(y + height, my[0] + mode.height()) - Math.max(y, my[0]));

            overlap = xOverlap * yOverlap;
            if (bestoverlap < overlap) {
                bestoverlap = overlap;
                bestmonitor = monitor;
            }
        }

        return bestmonitor;
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

    public void resize(int width, int height) {
        glfwSetWindowSize(window, width, height);
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

    public void warpMouse(BiConsumer<Integer, Integer> deltaConsumer) {
        long monitor = getCurrentMonitor();
        GLFWVidMode properties = glfwGetVideoMode(monitor);
        int screenW = properties.width();
        int screenH = properties.height();

        int[] screenX = {0}, screenY = {0};
        glfwGetMonitorPos(monitor, screenX, screenY);

        float mx = mouseX * guiScale;
        float my = mouseY * guiScale;
        float actualX = mx - screenX[0] + x;
        float actualY = my - screenY[0] + y;

        if (actualX > 10 && actualX < screenW - 10 && actualY > 10 && actualY < screenH - 10)
            return;

        try {
            int dx = (int) ((screenW - 30) / guiScale);
            int dy = (int) ((screenH - 30) / guiScale);

            if (actualX <= 10) {
                LOGGER.debug("mouse warp left");
                glfwSetCursorPos(window, screenX[0] + screenW - x - 20, my);
                deltaConsumer.accept(dx, 0);
            } else if (actualX >= screenW - 10) {
                LOGGER.debug("mouse warp right");
                glfwSetCursorPos(window, screenX[0] - x + 20, my);
                deltaConsumer.accept(-dx, 0);
            } else if (actualY <= 10) {
                LOGGER.debug("mouse warp up");
                glfwSetCursorPos(window, mx, screenY[0] + screenH - y - 20);
                deltaConsumer.accept(0, dy);
            } else { //if (actualY >= screenH - 10)
                LOGGER.debug("mouse warp down");
                glfwSetCursorPos(window, mx, screenY[0] - y + 20);
                deltaConsumer.accept(0, -dy);
            }
        } catch (Exception ignored) {}
    }

    public long getHandle() {
        return window;
    }
}
