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

    protected final Map<String, Runnable> tickActions = new HashMap<>();
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
     * Takes up to 6 directional {@link Keybind} and process as a directional {@link org.joml.Vector3f} split into {@code x, y, z} components<br>
     * @param name A unique name for this action
     * @param left The {@link Keybind} for left movement
     * @param right The {@link Keybind} for right movement
     * @param up The {@link Keybind} for up movement
     * @param down The {@link Keybind} for down movement
     * @param forward The {@link Keybind} for forward movement
     * @param backward The {@link Keybind} for backward movement
     * @param action The action to run every tick with the directional vector
     * @return This controller
     * @see #bindVector3D(String, Keybind, Keybind, Keybind, TriConsumer)
     */
    public Controller bindVector3D(String name, Keybind left, Keybind right, Keybind up, Keybind down, Keybind forward, Keybind backward, TriConsumer<Float, Float, Float> action) {
        if (left    != null) keybinds.add(left);     if (right    != null) keybinds.add(right);
        if (up      != null) keybinds.add(up);       if (down     != null) keybinds.add(down);
        if (forward != null) keybinds.add(forward);  if (backward != null) keybinds.add(backward);
        tickActions.put(name, () -> {
            float x = 0, y = 0, z = 0;
            if (left     != null && left.isPressed())     x -= left.getAxisValue();
            if (right    != null && right.isPressed())    x += right.getAxisValue();
            if (down     != null && down.isPressed())     y -= down.getAxisValue();
            if (up       != null && up.isPressed())       y += up.getAxisValue();
            if (backward != null && backward.isPressed()) z -= backward.getAxisValue();
            if (forward  != null && forward.isPressed())  z += forward.getAxisValue();

            if (Vector3f.lengthSquared(x, y, z) > 0)
                action.accept(x, y, z);
        });
        return this;
    }

    /**
     * Triggers every tick, reading from three analog joystick axes to create a 3D vector
     * @param name A unique name for this action
     * @param axisX The {@link Keybind} for the X axis
     * @param axisY The {@link Keybind} for the Y axis
     * @param axisZ The {@link Keybind} for the Z axis
     * @param action The action to run every tick with the axis vector
     * @return This controller
     * @see #bindVector3D(String, Keybind, Keybind, Keybind, Keybind, Keybind, Keybind, TriConsumer)
     */
    public Controller bindVector3D(String name, Keybind axisX, Keybind axisY, Keybind axisZ, TriConsumer<Float, Float, Float> action) {
        if (axisX != null) keybinds.add(axisX);
        if (axisY != null) keybinds.add(axisY);
        if (axisZ != null) keybinds.add(axisZ);
        tickActions.put(name, () -> {
            float x = axisX != null ? axisX.getAxisValue() : 0;
            float y = axisY != null ? axisY.getAxisValue() : 0;
            float z = axisZ != null ? axisZ.getAxisValue() : 0;

            if (x != 0 || y != 0 || z != 0)
                action.accept(x, y, z);
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
     * @see #bindVector2D(String, Keybind, Keybind, BiConsumer)
     */
    public Controller bindVector2D(String name, Keybind left, Keybind right, Keybind up, Keybind down, BiConsumer<Float, Float> action) {
        if (left != null) keybinds.add(left); if (right != null) keybinds.add(right);
        if (up   != null) keybinds.add(up);   if (down  != null) keybinds.add(down);
        tickActions.put(name, () -> {
            float x = 0, y = 0;
            if (left  != null && left.isPressed())  x -= left.getAxisValue();
            if (right != null && right.isPressed()) x += right.getAxisValue();
            if (down  != null && down.isPressed())  y -= down.getAxisValue();
            if (up    != null && up.isPressed())    y += up.getAxisValue();

            if (Vector2f.lengthSquared(x, y) > 0)
                action.accept(x, y);
        });
        return this;
    }

    /**
     * Triggers every tick, reading from two analog joystick axes to create a 2D vector
     * @param name A unique name for this action
     * @param axisX The {@link Keybind} for the X axis
     * @param axisY The {@link Keybind} for the Y axis
     * @param action The action to run every tick with the axis vector
     * @return This controller
     * @see #bindVector2D(String, Keybind, Keybind, Keybind, Keybind, BiConsumer)
     */
    public Controller bindVector2D(String name, Keybind axisX, Keybind axisY, BiConsumer<Float, Float> action) {
        if (axisX != null) keybinds.add(axisX);
        if (axisY != null) keybinds.add(axisY);
        tickActions.put(name, () -> {
            float x = axisX != null ? axisX.getAxisValue() : 0;
            float y = axisY != null ? axisY.getAxisValue() : 0;

            if (x != 0 || y != 0)
                action.accept(x, y);
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
     * Processes all registered actions and calls them with the appropriate values<br>
     * <br>
     * Call this every tick to process input
     */
    public void tick() {
        for (Runnable action : tickActions.values())
            action.run();

        for (Keybind keybind : keybinds)
            keybind.polled();
    }

    /**
     * Clears all registered actions
     */
    public void clearActions() {
        tickActions.clear();
        keybinds.clear();
    }

    /**
     * Removes a registered action by name
     * @param name The name of the action to remove
     */
    public void removeAction(String name) {
        tickActions.remove(name);
    }
}