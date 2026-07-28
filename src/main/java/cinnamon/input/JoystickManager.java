package cinnamon.input;

import cinnamon.Client;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import static cinnamon.Client.LOGGER;
import static org.lwjgl.glfw.GLFW.*;

public class JoystickManager {

    private static final Joystick[] joysticks = new Joystick[GLFW_JOYSTICK_LAST + 1];
    private static int joystickCount;

    static {
        //check for connected joysticks
        for (int jid = 0; jid <= GLFW_JOYSTICK_LAST; jid++) {
            if (glfwJoystickPresent(jid))
                initializeJoystick(jid);
        }
    }

    public static void poll() {
        if (joystickCount <= 0)
            return;

        //poll controls
        for (Joystick joystick : joysticks) {
            if (joystick != null)
                joystick.poll();
        }
    }

    public static void joystickConnectEvent(int jid, int event) {
        System.out.println("Joystick event: " + jid + " " + event);
        if (event == GLFW_CONNECTED) {
            initializeJoystick(jid);
        } else if (event == GLFW_DISCONNECTED) {
            disconnectJoystick(jid);
        }
    }

    private static void initializeJoystick(int jid) {
        Joystick j = new Joystick(jid, glfwGetGamepadName(jid));
        LOGGER.info("Joystick connected: %s (ID %s)", j.name, j.jid);
        joysticks[jid] = j;
        joystickCount++;
    }

    private static void disconnectJoystick(int jid) {
        Joystick j = joysticks[jid];
        if (j == null)
            return;

        LOGGER.info("Joystick disconnected: %s (ID %s)", j.name, j.jid);
        joysticks[jid] = null;
        joystickCount--;
    }

    public static Joystick getJoystick(int jid) {
        return jid < 0 || jid > joysticks.length ? null : joysticks[jid];
    }

    public static int getJoystickCount() {
        return joystickCount;
    }

    public static class Joystick {
        public final int jid;
        public final String name;

        private boolean[] buttons = new boolean[0];
        private float[] axes = new float[0];
        private byte[] hats = new byte[0];

        public Joystick(int jid, String name) {
            this.jid = jid;
            this.name = name == null ? "Unknown Joystick" : name;
        }

        public void poll() {
            if (!glfwJoystickPresent(jid))
                return;

            Client c = Client.getInstance();

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
    }
}