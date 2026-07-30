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

public class Controller {

    private static final Vector3f tempDir3 = new Vector3f();
    private static final Vector2f
            tempDir2 = new Vector2f(),
            tempMouseDelta = new Vector2f(),
            tempMouseScroll = new Vector2f();

    private static double mouseX, mouseY, offsetX, offsetY;
    private static boolean firstMouse = true;

    private final Map<String, Runnable> tickActions = new HashMap<>();
    private final Map<String, BiConsumer<Float, Float>>
            mouseMoveActions = new HashMap<>(),
            mouseScrollActions = new HashMap<>();

    private final List<Keybind> keybinds = new ArrayList<>();

    /**
     * triggers once when the keybind is clicked (pressed down)
     * @param name a unique name for this action
     * @param keybind the keybind to listen for
     * @param action the action to run when the keybind is clicked
     * @return this controller
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
     * passes a boolean every tick indicating if the keybind is currently held down
     * @param name a unique name for this action
     * @param keybind the keybind to listen for
     * @param action the action to run every tick with the keybind state
     * @return this controller
     */
    public Controller bindState(String name, Keybind keybind, Consumer<Boolean> action) {
        keybinds.add(keybind);
        tickActions.put(name, () -> action.accept(keybind.isPressed()));
        return this;
    }

    /**
     * takes up to 6 directional keybinds and passes a {@link org.joml.Vector3f}
     * @param name a unique name for this action
     * @param left the keybind for left movement
     * @param right the keybind for right movement
     * @param up the keybind for up movement
     * @param down the keybind for down movement
     * @param forward the keybind for forward movement
     * @param backward the keybind for backward movement
     * @param action the action to run every tick with the directional vector
     * @return this controller
     */
    public Controller bindVector3D(String name, Keybind left, Keybind right, Keybind up, Keybind down, Keybind forward, Keybind backward, TriConsumer<Float, Float, Float> action) {
        keybinds.add(left);    keybinds.add(right);
        keybinds.add(up);      keybinds.add(down);
        keybinds.add(forward); keybinds.add(backward);
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
     * takes up to 4 directional keybinds and passes a {@link org.joml.Vector2f}
     * @param name a unique name for this action
     * @param left the keybind for left movement
     * @param right the keybind for right movement
     * @param up the keybind for up movement
     * @param down the keybind for down movement
     * @param action the action to run every tick with the directional vector
     * @return this controller
     */
    public Controller bindVector2D(String name, Keybind left, Keybind right, Keybind up, Keybind down, BiConsumer<Float, Float> action) {
        keybinds.add(left); keybinds.add(right);
        keybinds.add(up);   keybinds.add(down);
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
     * registers a listener for mouse delta movements
     * @param name a unique name for this action
     * @param action the action to run every tick with the mouse delta
     * @return this controller
     */
    public Controller bindMouseMove(String name, BiConsumer<Float, Float> action) {
        mouseMoveActions.put(name, action);
        return this;
    }

    /**
     * registers a listener for mouse scroll movements
     * @param name a unique name for this action
     * @param action the action to run every tick with the mouse scroll delta
     * @return this controller
     */
    public Controller bindMouseScroll(String name, BiConsumer<Float, Float> action) {
        mouseScrollActions.put(name, action);
        return this;
    }

    /**
     * processes all registered actions and calls them with the appropriate values<br>
     * call this every tick to process input
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
     * clears the mouse delta and scroll values<br>
     * call after every tick to avoid accumulating values over multiple ticks
     */
    public static void clearTick() {
        tempMouseDelta.set(0);
        tempMouseScroll.set(0);
    }

    /**
     * clears all registered actions
     */
    public void clearActions() {
        tickActions.clear();
        mouseMoveActions.clear();
        mouseScrollActions.clear();
        keybinds.clear();
    }

    /**
     * removes a registered action by name
     * @param name the name of the action to remove
     */
    public void removeAction(String name) {
        tickActions.remove(name);
        mouseMoveActions.remove(name);
        mouseScrollActions.remove(name);
    }

    /**
     * hook for mouse movement callback
     * @param x the current mouse x position
     * @param y the current mouse y position
     */
    public static void mouseMove(double x, double y) {
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
        double dx = offsetX * spd * 0.15f;
        double dy = offsetY * spd * 0.15f;

        offsetX = 0;
        offsetY = 0;

        if (dx != 0 || dy != 0)
            tempMouseDelta.add((float) dx, (float) dy);
    }

    /**
     * hook for mouse scroll callback
     * @param x the scroll amount in the x direction
     * @param y the scroll amount in the y direction
     */
    public static void mouseScroll(double x, double y) {
        if (x != 0 || y != 0)
            tempMouseScroll.add((float) x, (float) y);
    }

    /**
     * reset state
     */
    public static void reset() {
        firstMouse = true;
        tempMouseDelta.set(0);
        tempMouseScroll.set(0);
    }
}