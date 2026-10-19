package cinnamon.input;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static cinnamon.input.Keybind.KeyType.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Controller Tests")
class ControllerTest {

    private Controller controller;

    @BeforeEach
    void setup() {
        //initialize a new controller for each test
        controller = new Controller();
    }

    @AfterEach
    void releaseInput() {
        //reset all keybinds after each test
        Keybind.releaseAll(true, true);
        Keybind.flush();
    }

    @Test
    @DisplayName("Click")
    void testClickBinding() {
        //create a new gamepad button
        Keybind keybind = new Keybind("test-key", 0, GAMEPAD_BUTTON);
        List<Integer> clicks = new ArrayList<>();

        controller.bindClick("click", keybind, clicks::add);

        //verify the first tick does not trigger the action
        controller.tick();
        assertTrue(clicks.isEmpty());

        //simulate a button press and verify the action is triggered
        Keybind.gamepadButtonPress(0, true, -1);
        controller.tick();
        assertEquals(List.of(1), clicks);

        //check if the action is not triggered again without a release
        controller.tick();
        assertEquals(List.of(1), clicks);

        //simulate a button release and verify the action is triggered again
        Keybind.gamepadButtonPress(0, false, -1);
        controller.tick();
        Keybind.gamepadButtonPress(0, true, -1);
        controller.tick();

        assertEquals(List.of(1, 1), clicks);
    }

    @Test
    @DisplayName("Key State Checking")
    void testStateBinding() {
        //create a new gamepad button
        Keybind keybind = new Keybind("test-key", 0, GAMEPAD_BUTTON);
        List<Boolean> states = new ArrayList<>();

        controller.bindState("state", keybind, states::add);

        //first tick -> false (not pressed)
        controller.tick();

        //press key and tick -> true (pressed)
        Keybind.gamepadButtonPress(0, true, -1);
        controller.tick();

        //release key and tick -> false (released)
        Keybind.gamepadButtonPress(0, false, -1);
        controller.tick();

        //verify the states recorded match the expected sequence
        assertEquals(List.of(false, true, false), states);
    }

    @Test
    @DisplayName("Double Click")
    void testDoubleClickBinding() {
        //create a new gamepad button
        Keybind keybind = new Keybind("test-key", 0, GAMEPAD_BUTTON);
        List<Boolean> doubleClicks = new ArrayList<>();

        controller.bindDoubleClick("double", keybind, doubleClicks::add);

        //initial controller tick
        controller.tick();

        //simulate a button press and release (click)
        Keybind.gamepadButtonPress(0, true, -1);
        controller.tick();
        Keybind.gamepadButtonPress(0, false, -1);
        controller.tick();

        //check if the click was registered as a double click (false -> single click)
        assertEquals(List.of(false), doubleClicks);

        //press again
        Keybind.gamepadButtonPress(0, true, -1);
        controller.tick();

        //check if it was registered as a double click
        assertEquals(List.of(false, true), doubleClicks);
    }

    @Test
    @DisplayName("Directional 2D")
    void testDirectionalVector2Binding() {
        //create directional gamepad buttons
        Keybind left  = new Keybind("test-key-left",  0, GAMEPAD_BUTTON);
        Keybind right = new Keybind("test-key-right", 1, GAMEPAD_BUTTON);
        Keybind up    = new Keybind("test-key-up",    2, GAMEPAD_BUTTON);
        Keybind down  = new Keybind("test-key-down",  3, GAMEPAD_BUTTON);
        List<float[]> values = new ArrayList<>();

        controller.bindVector2D("direction", left, right, up, down, (x, y) -> values.add(new float[]{x, y}));

        //verify the first tick does not trigger the action
        controller.tick();
        assertTrue(values.isEmpty());

        //simulate pressing directional buttons
        Keybind.gamepadButtonPress(1, true, -1);
        Keybind.gamepadButtonPress(3, true, -1);
        controller.tick();

        //verify the combined directional values
        assertEquals(1, values.size());
        assertArrayEquals(new float[]{1, -1}, values.getFirst());
    }

    @Test
    @DisplayName("Analog 2D")
    void testAnalogVector2Binding() {
        //create gamepad axes
        Keybind x = new Keybind("test-key-x", 0, GAMEPAD_AXIS);
        Keybind y = new Keybind("test-key-y", 1, GAMEPAD_AXIS);
        List<float[]> values = new ArrayList<>();

        controller.bindVector2D("analog", x, y, (xValue, yValue) -> values.add(new float[]{xValue, yValue}));

        //simulate axis movement and tick
        Keybind.gamepadAxisMove(0,  0.5f,  -1, 0);
        Keybind.gamepadAxisMove(1, -0.25f, -1, 0);
        controller.tick();

        //verify the analog values match the axis input
        assertEquals(1, values.size());
        assertArrayEquals(new float[]{0.5f, -0.25f}, values.getFirst());
    }

    @Test
    @DisplayName("Directional 3D")
    void testDirectionalVector3Binding() {
        //create directional gamepad buttons
        Keybind left    = new Keybind("test-key-left",    0, GAMEPAD_BUTTON);
        Keybind forward = new Keybind("test-key-forward", 1, GAMEPAD_BUTTON);
        List<float[]> values = new ArrayList<>();

        controller.bindVector3D("direction", left, null, null, null, forward, null, (x, y, z) -> values.add(new float[]{x, y, z}));

        //simulate pressing directional buttons
        Keybind.gamepadButtonPress(0, true, -1);
        Keybind.gamepadButtonPress(1, true, -1);
        controller.tick();

        //verify the combined directional values
        assertEquals(1, values.size());
        assertArrayEquals(new float[]{-1, 0, 1}, values.getFirst());
    }

    @Test
    @DisplayName("Analog 3D")
    void testAnalogVector3Binding() {
        //create gamepad axes
        Keybind x = new Keybind("test-key-x", 0, GAMEPAD_AXIS);
        Keybind z = new Keybind("test-key-z", 1, GAMEPAD_AXIS);
        List<float[]> values = new ArrayList<>();

        controller.bindVector3D("analog", x, null, z, (xValue, yValue, zValue) -> values.add(new float[]{xValue, yValue, zValue}));

        //simulate axis movement and tick
        Keybind.gamepadAxisMove(0,  0.75f, -1, 0);
        Keybind.gamepadAxisMove(1, -0.5f,  -1, 0);
        controller.tick();

        //verify the analog values match the axis input
        assertEquals(1, values.size());
        assertArrayEquals(new float[]{0.75f, 0, -0.5f}, values.getFirst());
    }

    @Test
    @DisplayName("Float")
    void testFloatBinding() {
        //create a new gamepad axis
        Keybind axis = new Keybind("test-key", 0, GAMEPAD_AXIS);
        List<float[]> values = new ArrayList<>();

        controller.bindFloat("float", axis, (current, last) -> values.add(new float[]{current, last}));

        //simulate axis movement and tick
        Keybind.gamepadAxisMove(0, 0.5f, -1, 0.25f);
        controller.tick();
        Keybind.flush();
        controller.tick();

        //verify current and previous values
        assertEquals(1, values.size());
        assertArrayEquals(new float[]{0.5f, 0.25f}, values.getFirst());
    }

    @Test
    @DisplayName("Action Lifecycle")
    void testActionLifecycle() {
        //create gamepad buttons
        Keybind first  = new Keybind("test-key-first",  0, GAMEPAD_BUTTON);
        Keybind second = new Keybind("test-key-second", 1, GAMEPAD_BUTTON);
        List<String> actions = new ArrayList<>();

        //bind multiple actions under the same name to verify replacement
        controller.bindState("action", first,  _ -> actions.add("first"));
        controller.bindState("action", second, _ -> actions.add("second"));
        assertEquals(1, controller.tickActions.size());
        assertEquals(2, controller.keybinds.size());

        //press button and tick to verify the replacement action executes
        Keybind.gamepadButtonPress(0, true, -1);
        Keybind.gamepadButtonPress(1, true, -1);
        controller.tick();
        assertEquals(List.of("second"), actions);

        //remove action and verify it no longer executes
        controller.removeAction("action");
        controller.tick();
        assertEquals(List.of("second"), actions);

        //clear all actions and verify controller state is reset
        controller.clearActions();
        assertTrue(controller.tickActions.isEmpty());
        assertTrue(controller.keybinds.isEmpty());
    }
}
