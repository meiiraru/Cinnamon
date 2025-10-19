package cinnamon.render;

import cinnamon.utils.Resource;
import cinnamon.utils.TextureIO;
import cinnamon.vr.XrManager;
import cinnamon.vr.XrRenderer;
import org.joml.Math;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.glfw.GLFWVidMode;

import java.util.function.BiConsumer;

import static cinnamon.Client.LOGGER;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * A window, the primary way to display the program's content to the user
 */
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
    public boolean allowFullscreen = true;

    //gui properties
    public int scaledWidth = width, scaledHeight = height;
    public float guiScale = 1f, maxGuiScale = 1f;

    //mouse properties
    public int mouseX, mouseY;
    private boolean mouseLocked;

    /**
     * Create a new window
     * @param window the window's GLFW handle
     * @param width the window's initial width
     * @param height the window's initial height
     */
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

    /**
     * Tells GLFW to close the window
     */
    public void exit() {
        glfwSetWindowShouldClose(window, true);
    }

    /**
     * Unlock the mouse from the window
     * @return true if the mouse was set to unlocked, false if it was already unlocked
     */
    public boolean unlockMouse() {
        if (!mouseLocked)
            return false;

        //unlock cursor
        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);

        //move cursor to the window's center
        setMousePos(width / 2f, height / 2f);

        //save state
        this.mouseLocked = false;

        return true;
    }

    /**
     * Lock the mouse inside the window (hide and center it)
     * @return true if the mouse was set to locked, false if it was already locked
     */
    public boolean lockMouse() {
        if (mouseLocked)
            return false;

        //lock cursor
        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);

        //save state
        this.mouseLocked = true;

        return true;
    }

    /**
     * Check if the mouse is locked inside the window (hidden and centered)
     * @return true if the mouse is locked
     */
    public boolean isMouseLocked() {
        return mouseLocked;
    }

    /**
     * Toggle the window's fullscreen state
     */
    public void toggleFullScreen() {
        if (!fullscreen && !allowFullscreen)
            return;

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

            glfwSetWindowMonitor(window, NULL, x, y, width, height, GLFW_DONT_CARE);
        }
    }

    /**
     * Check if the window is in fullscreen mode
     * @return true if the window is in fullscreen
     */
    public boolean isFullscreen() {
        return fullscreen;
    }

    /**
     * Get the monitor that has the most overlap with the window
     * @return the monitor's GLFW handle
     */
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

    /**
     * Set the window's title
     * @param title the new title
     */
    public void setTitle(String title) {
        glfwSetWindowTitle(window, title);
    }

    /**
     * Set the mouse's position
     * @param x the mouse x position
     * @param y the mouse y position
     */
    public void setMousePos(double x, double y) {
        glfwSetCursorPos(window, x, y);
        updateMosuePos(x, y);
    }

    /**
     * Update the mouse's position
     * @param x the mouse x position
     * @param y the mouse y position
     */
    public void updateMosuePos(double x, double y) {
        this.mouseX = (int) (x / guiScale);
        this.mouseY = (int) (y / guiScale);
    }

    /**
     * Update the window's position
     * @param x the new x position
     * @param y the new y position
     */
    public void updatePos(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Get the window's width in the GUI space
     * @return the window's GUI width
     */
    public int getGUIWidth() {
        return XrManager.isInXR() ? XrRenderer.GUI_WIDTH : scaledWidth;
    }

    /**
     * Get the window's height in the GUI space
     * @return the window's GUI height
     */
    public int getGUIHeight() {
        return XrManager.isInXR() ? XrRenderer.GUI_HEIGHT : scaledHeight;
    }

    /**
     * Update the window's dimensions and gui scale
     * @param width the new width (absolute)
     * @param height the new height (absolute)
     * @param guiScale the new gui scale (0 to auto-calculate)
     */
    public void updateSize(int width, int height, float guiScale) {
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

        if (XrManager.isInXR()) {
            this.guiScale = 1f;
            this.scaledWidth = XrRenderer.GUI_WIDTH;
            this.scaledHeight = XrRenderer.GUI_HEIGHT;
        }
    }

    /**
     * Resize the window to a new size
     * @param width the new absolute width
     * @param height the new absolute height
     */
    public void setSize(int width, int height) {
        glfwSetWindowSize(window, width, height);
    }

    /**
     * Set the window's position
     * @param x the new x position
     * @param y the new y position
     */
    public void setPos(int x, int y) {
        glfwSetWindowPos(window, x, y);
    }

    /**
     * Set the window's icon
     * @param resource the icon's resource path
     */
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

    /**
     * Warp the mouse to the opposite side of the screen if it's too close to the edge
     * @param deltaConsumer a consumer that takes how much the mouse was moved in x and y
     */
    public void warpMouse(BiConsumer<Integer, Integer> deltaConsumer) {
        if (XrManager.isInXR())
            return;

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
                setMousePos(screenX[0] + screenW - x - 20, my);
                deltaConsumer.accept(dx, 0);
            } else if (actualX >= screenW - 10) {
                LOGGER.debug("mouse warp right");
                setMousePos(screenX[0] - x + 20, my);
                deltaConsumer.accept(-dx, 0);
            } else if (actualY <= 10) {
                LOGGER.debug("mouse warp up");
                setMousePos(mx, screenY[0] + screenH - y - 20);
                deltaConsumer.accept(0, dy);
            } else { //if (actualY >= screenH - 10)
                LOGGER.debug("mouse warp down");
                setMousePos(mx, screenY[0] - y + 20);
                deltaConsumer.accept(0, -dy);
            }
        } catch (Exception ignored) {}
    }

    /**
     * Set the window's decorated state (border, title bar, close button, ...)
     * @param bool true if the window should be decorated (default true)
     */
    public void setDecorated(boolean bool) {
        glfwSetWindowAttrib(window, GLFW_DECORATED, bool ? GLFW_TRUE : GLFW_FALSE);
    }

    /**
     * Set if the window can be resizable
     * @param bool true if the window should be resizable (default true)
     */
    public void setResizable(boolean bool) {
        glfwSetWindowAttrib(window, GLFW_RESIZABLE, bool ? GLFW_TRUE : GLFW_FALSE);
    }

    /**
     * Set if the window is floating (always on top)
     * @param bool true if the window should be floating (default false)
     */
    public void setFloating(boolean bool) {
        glfwSetWindowAttrib(window, GLFW_FLOATING, bool ? GLFW_TRUE : GLFW_FALSE);
    }

    /**
     * Set the window cursor
     * @param cursor the cursor type, one of GLFW standard cursors:
     * <br><table><caption>GLFW standard cursors</caption><tr>
     * <td>{@link org.lwjgl.glfw.GLFW#GLFW_ARROW_CURSOR ARROW_CURSOR}</td>
     * <td>{@link org.lwjgl.glfw.GLFW#GLFW_IBEAM_CURSOR IBEAM_CURSOR}</td>
     * <td>{@link org.lwjgl.glfw.GLFW#GLFW_CROSSHAIR_CURSOR CROSSHAIR_CURSOR}</td>
     * <td>{@link org.lwjgl.glfw.GLFW#GLFW_POINTING_HAND_CURSOR POINTING_HAND_CURSOR}</td>
     * <td>{@link org.lwjgl.glfw.GLFW#GLFW_RESIZE_EW_CURSOR RESIZE_EW_CURSOR}</td>
     * </tr>
     * <tr><td>{@link org.lwjgl.glfw.GLFW#GLFW_RESIZE_NS_CURSOR RESIZE_NS_CURSOR}</td>
     * <td>{@link org.lwjgl.glfw.GLFW#GLFW_RESIZE_NWSE_CURSOR RESIZE_NWSE_CURSOR}</td>
     * <td>{@link org.lwjgl.glfw.GLFW#GLFW_RESIZE_NESW_CURSOR RESIZE_NESW_CURSOR}</td>
     * <td>{@link org.lwjgl.glfw.GLFW#GLFW_RESIZE_ALL_CURSOR RESIZE_ALL_CURSOR}</td>
     * <td>{@link org.lwjgl.glfw.GLFW#GLFW_NOT_ALLOWED_CURSOR NOT_ALLOWED_CURSOR}</td>
     * </tr></table>
     */
    public void setCursor(int cursor) {
        long cur = glfwCreateStandardCursor(cursor);
        if (cur == NULL) {
            LOGGER.error("Failed to set cursor %s", cursor);
            return;
        }

        glfwSetCursor(window, cur);
    }

    /**
     * Enable or disable VSync
     * @param enabled true to enable VSync, false to disable it
     */
    public void toggleVsync(boolean enabled) {
        glfwSwapInterval(enabled ? 1 : 0);
    }

    /**
     * Get the window's internal GLFW handle
     * @return the window's handle
     */
    public long getHandle() {
        return window;
    }
}
