package cinnamon.input;

import cinnamon.settings.Settings;
import cinnamon.utils.TriConsumer;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Controller class for handling input actions and keybinds
 */
public class Controller {

    protected static final Vector3f tempDir3 = new Vector3f();
    protected static final Vector2f
            tempDir2 = new Vector2f(),
            tempMouseDelta = new Vector2f(),
            tempMouseScroll = new Vector2f();

    protected static double mouseX, mouseY;
    protected static boolean firstMouse = true;

    protected final Map<String, Runnable> tickActions = new HashMap<>();
    protected final Map<String, BiConsumer<Float, Float>>
            mouseMoveActions = new HashMap<>(),
            mouseScrollActions = new HashMap<>();

    protected final List<Keybind> keybinds = new ArrayList<>();

    /**
     * Triggers once when the {@link Keybind} is clicked (pressed down)
     * @param name A unique name for this action
     * @param keybind The {@link Keybind} to listen for
     * @param action The action to run when the {@link Keybind} is clicked
     * @return This controller
     */
    public Controller bindClick(String name, Keybind keybind, Consumer<Integer> action) {
        keybinds.add(keybind);
        tickActions.put(name, () -> {
            if (keybind.click())
                action.accept(keybind.getClicks() + 1);
        });
        return this;
    }

    /**
     * Triggers every tick, checking if the {@link Keybind} is pressed<br>
     * The action takes a {@code boolean} every tick indicating if the {@link Keybind} is currently held down
     * @param name A unique name for this action
     * @param keybind The {@link Keybind} to listen for
     * @param action The action to run every tick with the {@link Keybind} state
     * @return This controller
     */
    public Controller bindState(String name, Keybind keybind, Consumer<Boolean> action) {
        keybinds.add(keybind);
        tickActions.put(name, () -> action.accept(keybind.isPressed()));
        return this;
    }

    /**
     * Triggers once when the {@link Keybind} is double-clicked (pressed down twice within a certain time frame)<br>
     * The action will be passed a {@code boolean} indicating if it was a double click or a single click<br>
     * The action takes {@code true} if it was a double click, and {@code false} if it was a single click
     * @param name a unique name for this action
     * @param keybind the {@link Keybind} to listen for
     * @param action the action to run when the keybind is clicked, with a boolean indicating if it was a double click
     * @return this controller
     */
    public Controller bindDoubleClick(String name, Keybind keybind, Consumer<Boolean> action) {
        //check if the keybind is pressed for the first time, if so, start a timer for double click detection
        final boolean[] pressed = {false};
        final int[] doubleClickTimer = {0};

        keybinds.add(keybind);
        tickActions.put(name, () -> {
            if (doubleClickTimer[0] > 0)
                doubleClickTimer[0]--;

            if (!pressed[0] && keybind.click()) {
                if (doubleClickTimer[0] > 0) {
                    doubleClickTimer[0] = 0;
                    action.accept(true);
                } else {
                    doubleClickTimer[0] = Settings.doubleKeypressTime.get();
                    action.accept(false);
                }
            }

            pressed[0] = keybind.isActuallyPressed();
        });
        return this;
    }

    /**
     * Triggers every tick, checking if the {@link Keybind} is pressed<br>
     * Takes up to 6 directional {@link Keybind} and process as an directional {@link org.joml.Vector3f} split into {@code x, y, z} components<br>
     * @param name A unique name for this action
     * @param left The {@link Keybind} for left movement
     * @param right The {@link Keybind} for right movement
     * @param up The {@link Keybind} for up movement
     * @param down The {@link Keybind} for down movement
     * @param forward The {@link Keybind} for forward movement
     * @param backward The {@link Keybind} for backward movement
     * @param action The action to run every tick with the directional vector
     * @return This controller
     * @see #bindVector2D(String, Keybind, Keybind, Keybind, Keybind, BiConsumer)
     */
    public Controller bindVector3D(String name, Keybind left, Keybind right, Keybind up, Keybind down, Keybind forward, Keybind backward, TriConsumer<Float, Float, Float> action) {
        if (left    != null) keybinds.add(left);     if (right    != null) keybinds.add(right);
        if (up      != null) keybinds.add(up);       if (down     != null) keybinds.add(down);
        if (forward != null) keybinds.add(forward);  if (backward != null) keybinds.add(backward);
        tickActions.put(name, () -> {
            tempDir3.set(0);
            if (left     != null && left.isPressed())     tempDir3.x -= 1;
            if (right    != null && right.isPressed())    tempDir3.x += 1;
            if (down     != null && down.isPressed())     tempDir3.y -= 1;
            if (up       != null && up.isPressed())       tempDir3.y += 1;
            if (backward != null && backward.isPressed()) tempDir3.z -= 1;
            if (forward  != null && forward.isPressed())  tempDir3.z += 1;

            if (tempDir3.lengthSquared() > 0)
                action.accept(tempDir3.x, tempDir3.y, tempDir3.z);
        });
        return this;
    }

    /**
     * Triggers every tick, checking if the {@link Keybind} is pressed<br>
     * Takes up to 4 directional {@link Keybind} and process as an directional {@link org.joml.Vector2f} split into {@code x, y} components<br>
     * @param name A unique name for this action
     * @param left The {@link Keybind} for left movement
     * @param right The {@link Keybind} for right movement
     * @param up The {@link Keybind} for up movement
     * @param down The {@link Keybind} for down movement
     * @param action The action to run every tick with the directional vector
     * @return This controller
     * @see #bindVector3D(String, Keybind, Keybind, Keybind, Keybind, Keybind, Keybind, TriConsumer)
     */
    public Controller bindVector2D(String name, Keybind left, Keybind right, Keybind up, Keybind down, BiConsumer<Float, Float> action) {
        if (left != null) keybinds.add(left); if (right != null) keybinds.add(right);
        if (up   != null) keybinds.add(up);   if (down  != null) keybinds.add(down);
        tickActions.put(name, () -> {
            tempDir2.set(0);
            if (left  != null && left.isPressed())  tempDir2.x -= 1;
            if (right != null && right.isPressed()) tempDir2.x += 1;
            if (down  != null && down.isPressed())  tempDir2.y -= 1;
            if (up    != null && up.isPressed())    tempDir2.y += 1;

            if (tempDir2.lengthSquared() > 0)
                action.accept(tempDir2.x, tempDir2.y);
        });
        return this;
    }

    /**
     * Triggers when the {@link Keybind} have a different axis value than the last tick<br>
     * The action takes the current axis value and the last axis value as parameters
     * @param name A unique name for this action
     * @param keybind The {@link Keybind} to listen for
     * @param action The action to run every tick with the current and last axis values
     * @return This controller
     */
    public Controller bindFloat(String name, Keybind keybind, BiConsumer<Float, Float> action) {
        keybinds.add(keybind);
        tickActions.put(name, () -> {
            float curr = keybind.getAxisValue();
            float last = keybind.getLastAxisValue();
            if (curr != last)
                action.accept(curr, last);
        });
        return this;
    }

    /**
     * Registers a listener for mouse delta movements
     * @param name A unique name for this action
     * @param action The action to run every tick with the mouse delta
     * @return This controller
     */
    public Controller bindMouseMove(String name, BiConsumer<Float, Float> action) {
        mouseMoveActions.put(name, action);
        return this;
    }

    /**
     * Registers a listener for mouse scroll movements
     * @param name A unique name for this action
     * @param action The action to run every tick with the mouse scroll delta
     * @return This controller
     */
    public Controller bindMouseScroll(String name, BiConsumer<Float, Float> action) {
        mouseScrollActions.put(name, action);
        return this;
    }

    /**
     * Processes all registered actions and calls them with the appropriate values<br>
     * <br>
     * Call this every tick to process input
     */
    public void tick() {
        for (Runnable action : tickActions.values())
            action.run();

        if (tempMouseDelta.lengthSquared() > 0) {
            for (BiConsumer<Float, Float> mouseMoveAction : mouseMoveActions.values())
                mouseMoveAction.accept(tempMouseDelta.x, tempMouseDelta.y);
        }

        if (tempMouseScroll.lengthSquared() > 0) {
            for (BiConsumer<Float, Float> mouseScrollAction : mouseScrollActions.values())
                mouseScrollAction.accept(tempMouseScroll.x, tempMouseScroll.y);
        }

        for (Keybind keybind : keybinds)
            keybind.polled();
    }

    /**
     * Clears the mouse delta and scroll values<br>
     * <br>
     * Call after every tick to avoid accumulating values over multiple ticks
     */
    public static void clearTick() {
        tempMouseDelta.set(0);
        tempMouseScroll.set(0);
    }

    /**
     * Clears all registered actions
     */
    public void clearActions() {
        tickActions.clear();
        mouseMoveActions.clear();
        mouseScrollActions.clear();
        keybinds.clear();
    }

    /**
     * Removes a registered action by name
     * @param name The name of the action to remove
     */
    public void removeAction(String name) {
        tickActions.remove(name);
        mouseMoveActions.remove(name);
        mouseScrollActions.remove(name);
    }

    /**
     * Hook for mouse movement callback
     * @param x The current mouse {@code x} position
     * @param y The current mouse {@code y} position
     */
    public static void mouseMove(double x, double y) {
        if (firstMouse) {
            mouseX = x;
            mouseY = y;
            firstMouse = false;
        }

        double dx = (x - mouseX) * (Settings.invertX.get() ? -1 : 1);
        double dy = (y - mouseY) * (Settings.invertY.get() ? -1 : 1);
        mouseX = x;
        mouseY = y;

        double sensi = InputManager.getSensiMultiplier();
        dx *= sensi;
        dy *= sensi;

        if (dx != 0 || dy != 0)
            tempMouseDelta.add((float) dx, (float) dy);
    }

    /**
     * Hook for mouse scroll callback
     * @param x the scroll amount in the {@code x} direction
     * @param y the scroll amount in the {@code y} direction
     */
    public static void mouseScroll(double x, double y) {
        if (x != 0 || y != 0)
            tempMouseScroll.add((float) x, (float) y);
    }

    /**
     * Reset state
     */
    public static void reset() {
        firstMouse = true;
        tempMouseDelta.set(0);
        tempMouseScroll.set(0);
    }
}