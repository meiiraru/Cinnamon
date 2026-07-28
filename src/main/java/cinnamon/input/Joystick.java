package cinnamon.input;

import cinnamon.Client;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import static org.lwjgl.glfw.GLFW.*;

public class Joystick {
    private final int jid;
    private final String name;
    private final boolean gamepad;

    private boolean[] buttons = new boolean[0];
    private float[] axes = new float[0];
    private byte[] hats = new byte[0];

    private final boolean[] gamepadButtons = new boolean[GLFW_GAMEPAD_BUTTON_LAST + 1];
    private final float[] gamepadAxes = new float[GLFW_GAMEPAD_AXIS_LAST + 1];

    public Joystick(int jid, String name, boolean gamepad) {
        this.jid = jid;
        this.name = name == null ? "Unknown Joystick" : name;
        this.gamepad = gamepad;
    }

    public boolean poll() {
        if (!glfwJoystickPresent(jid))
            return false;

        Client c = Client.getInstance();

        if (isGamepad())
            pollGamepad(c);
        pollJoystick(c);

        return true;
    }

    private void pollGamepad(Client c) {
        if (!glfwGetGamepadState(jid, JoystickManager.gamepadState))
            return;

        //poll buttons
        for (int i = 0; i < gamepadButtons.length; i++) {
            boolean butt = JoystickManager.gamepadState.buttons(i) == GLFW_PRESS;
            if (gamepadButtons[i] != butt) {
                gamepadButtons[i] = butt;
                c.gamepadButtonPress(i, butt, jid);
            }
        }
        //poll axes
        for (int i = 0; i < gamepadAxes.length; i++) {
            float axis = JoystickManager.gamepadState.axes(i);
            if (gamepadAxes[i] != axis) {
                float prev = gamepadAxes[i];
                gamepadAxes[i] = axis;
                c.gamepadAxisMove(i, axis, jid, prev);
            }
        }
    }

    private void pollJoystick(Client c) {
        //poll buttons
        ByteBuffer buttonBuffer = glfwGetJoystickButtons(jid);
        if (buttonBuffer != null) {
            int size = buttonBuffer.capacity();
            //grow array
            if (buttons.length < size) {
                boolean[] newButtons = new boolean[size];
                System.arraycopy(buttons, 0, newButtons, 0, buttons.length);
                buttons = newButtons;
            }

            //state changes
            for (int i = 0; i < size; i++) {
                boolean butt = buttonBuffer.get(i) == GLFW_PRESS;
                if (buttons[i] != butt) {
                    buttons[i] = butt;
                    c.joystickButtonPress(i, butt, jid);
                }
            }
        }

        //poll axes
        FloatBuffer axisBuffer = glfwGetJoystickAxes(jid);
        if (axisBuffer != null) {
            int size = axisBuffer.capacity();
            //grow array
            if (axes.length < size) {
                float[] newAxes = new float[size];
                System.arraycopy(axes, 0, newAxes, 0, axes.length);
                axes = newAxes;
            }

            //state changes
            for (int i = 0; i < size; i++) {
                float axis = axisBuffer.get(i);
                if (axes[i] != axis) {
                    float prev = axes[i];
                    axes[i] = axis;
                    c.joystickAxisMove(i, axis, jid, prev);
                }
            }
        }

        //poll hats (D-Pads)
        ByteBuffer hatBuffer = glfwGetJoystickHats(jid);
        if (hatBuffer != null) {
            int size = hatBuffer.capacity();
            //grow array
            if (hats.length < size) {
                byte[] newHats = new byte[size];
                System.arraycopy(hats, 0, newHats, 0, hats.length);
                hats = newHats;
            }

            //state changes
            for (int i = 0; i < size; i++) {
                byte hatState = hatBuffer.get(i);
                if (hats[i] != hatState) {
                    byte prev = hats[i];
                    hats[i] = hatState;
                    c.joystickHatMove(i, hatState, jid, prev);
                }
            }
        }
    }

    public boolean getButtonState(int button) {
        return button >= 0 && button < buttons.length && buttons[button];
    }

    public float getAxisState(int axis) {
        return axis >= 0 && axis < axes.length ? axes[axis] : 0f;
    }

    public byte getHatState(int hat) {
        return hat >= 0 && hat < hats.length ? hats[hat] : GLFW_HAT_CENTERED;
    }

    public boolean getGamepadButtonState(int button) {
        return button >= 0 && button < gamepadButtons.length && gamepadButtons[button];
    }

    public float getGamepadAxisState(int axis) {
        return axis >= 0 && axis < gamepadAxes.length ? gamepadAxes[axis] : 0f;
    }

    public int getNumberOfButtons() {
        return buttons.length;
    }

    public int getNumberOfAxes() {
        return axes.length;
    }

    public int getNumberOfHats() {
        return hats.length;
    }

    public int getJoystickID() {
        return jid;
    }

    public String getName() {
        return name;
    }

    public boolean isGamepad() {
        return gamepad;
    }
}
