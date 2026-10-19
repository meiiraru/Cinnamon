package cinnamon.input;

import cinnamon.settings.Settings;
import cinnamon.text.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static cinnamon.Client.LOGGER;
import static org.lwjgl.glfw.GLFW.*;

public class Keybind {

    private static final Map<KeyType, List<Keybind>> KEYBINDS;

    static {
        KEYBINDS = new HashMap<>();
        for (KeyType type : KeyType.values())
            KEYBINDS.put(type, new ArrayList<>());
    }

    private static float mouseX, mouseY;
    private static boolean firstMouse = true;

    private final String name;

    private int key, mods;
    private KeyType type;

    private Text text;

    private boolean polled;
    private boolean pressed;

    //generic button
    private boolean pollPressed;
    private int clicks;

    //joystick exclusive
    private int joystick;
    private float lastAxisValue;
    private float axisValue;

    public Keybind(String name, int key, KeyType type) {
        this(name, key, 0, type);
    }

    public Keybind(String name, int key, int mods, KeyType type) {
        this(name, key, mods, type, -1);
    }

    public Keybind(String name, int key, int mods, KeyType type, int joystick) {
        this.name = name;
        this.key  = key;
        this.mods = mods;
        this.type = type;
        this.joystick = joystick;
        this.text = null;
        KEYBINDS.get(type).add(this);
        LOGGER.debug("Registered %s keybind: %s %s", type.name(), key, name);
    }

    public static void unregister(Keybind keybind) {
        KEYBINDS.get(keybind.type).remove(keybind);
    }

    public static void keyPress(int key, int scancode, int action, int mods) {
        if (key == -1 && scancode == -1)
            return;

        boolean pressed = action != GLFW_RELEASE;

        if (key != -1) {
            for (Keybind keybind : KEYBINDS.get(KeyType.KEY)) {
                if (keybind.key == key)
                    processKeybind(keybind, pressed, mods);
            }
        } else {
            for (Keybind keybind : KEYBINDS.get(KeyType.SCANCODE)) {
                if (keybind.key == scancode)
                    processKeybind(keybind, pressed, mods);
            }
        }
    }

    public static void mousePress(int button, int action, int mods) {
        if (button == -1)
            return;

        boolean pressed = action != GLFW_RELEASE;
        for (Keybind keybind : KEYBINDS.get(KeyType.MOUSE)) {
            if (keybind.key == button)
                processKeybind(keybind, pressed, mods);
        }
    }

    public static void mouseMove(float x, float y) {
        if (firstMouse) {
            mouseX = x;
            mouseY = y;
            firstMouse = false;
        }

        float dx = (x - mouseX) * (Settings.invertX.get() ? -1 : 1);
        float dy = (y - mouseY) * (Settings.invertY.get() ? -1 : 1);
        mouseX = x;
        mouseY = y;

        float sensi = (float) InputManager.getSensiMultiplier();
        dx *= sensi;
        dy *= sensi;

        for (Keybind keybind : KEYBINDS.get(KeyType.MOUSE_MOVE)) {
            float delta = (keybind.key == 0) ? dx : dy;
            processAxis(keybind, keybind.axisValue + delta, keybind.axisValue, 0);
        }
    }

    public static void mouseScroll(float x, float y) {
        //key: 0 -> left, 1 -> right, 2 -> up, 3 -> down
        int key = (x < 0) ? 1 : (x > 0) ? 0 : (y < 0) ? 3 : (y > 0) ? 2 : -1;
        if (key == -1)
            return;

        float delta = Math.abs((key < 2) ? x : y);

        for (Keybind keybind : KEYBINDS.get(KeyType.MOUSE_SCROLL)) {
            if (keybind.key == key)
                processAxis(keybind, keybind.axisValue + delta, keybind.axisValue, 0);
        }
    }

    public static void gamepadButtonPress(int button, boolean pressed, int joystick) {
        for (Keybind keybind : KEYBINDS.get(KeyType.GAMEPAD_BUTTON)) {
            if (keybind.key == button && keybind.joystick == joystick)
                processKeybind(keybind, pressed, 0);
        }
    }

    public static void gamepadAxisMove(int axis, float value, int joystick, float lastValue) {
        float deadzone = Settings.gamepadDeadzone.get();
        for (Keybind keybind : KEYBINDS.get(KeyType.GAMEPAD_AXIS)) {
            if (keybind.key == axis && keybind.joystick == joystick)
                processAxis(keybind, value, lastValue, deadzone);
        }
    }

    public static void xrButtonPress(int button, boolean pressed, int hand) {
        for (Keybind keybind : KEYBINDS.get(KeyType.XR_BUTTON)) {
            if (keybind.key == button && keybind.joystick == hand)
                processKeybind(keybind, pressed, 0);
        }
    }

    public static void xrTriggerPress(int button, float value, int hand, float lastValue) {
        float deadzone = Settings.gamepadDeadzone.get();
        for (Keybind keybind : KEYBINDS.get(KeyType.XR_TRIGGER)) {
            if (keybind.key == button && keybind.joystick == hand)
                processAxis(keybind, value, lastValue, deadzone);
        }
    }

    public static void xrJoystickMove(float x, float y, int hand, float lastX, float lastY) {
        float deadzone = Settings.gamepadDeadzone.get();
        for (Keybind keybind : KEYBINDS.get(KeyType.XR_JOYSTICK)) {
            if (keybind.joystick == hand) {
                if (keybind.key == 0) //x = axis 0
                    processAxis(keybind, x, lastX, deadzone);
                else if (keybind.key == 1) //y = axis 1
                    processAxis(keybind, y, lastY, deadzone);
            }
        }
    }

    public static void joystickButtonPress(int button, boolean pressed, int joystick) {
        for (Keybind keybind : KEYBINDS.get(KeyType.JOYSTICK_BUTTON)) {
            if (keybind.key == button && keybind.joystick == joystick)
                processKeybind(keybind, pressed, 0);
        }
    }

    public static void joystickAxisMove(int axis, float value, int joystick, float lastValue) {
        float deadzone = Settings.gamepadDeadzone.get();
        for (Keybind keybind : KEYBINDS.get(KeyType.JOYSTICK_AXIS)) {
            if (keybind.key == axis && keybind.joystick == joystick)
                processAxis(keybind, value, lastValue, deadzone);
        }
    }

    public static void joystickHatMove(int hat, byte hatState, int joystick, byte lastValue) {
        for (Keybind keybind : KEYBINDS.get(KeyType.JOYSTICK_HAT)) {
            //unpack encoded key - upper bits = hat ID, lower 4 bits = direction flag (1, 2, 4, 8)
            if ((keybind.key >> 4) == hat && keybind.joystick == joystick) {
                int direction = keybind.key & 0xF;
                boolean pressed = (hatState & direction) != 0;

                //only trigger if state changed to prevent multiple press events while sweeping
                if (pressed != keybind.pressed)
                    processKeybind(keybind, pressed, 0);
            }
        }
    }

    private static void processKeybind(Keybind keybind, boolean pressed, int mods) {
        keybind.pressed = pressed;
        if (pressed) {
            if ((mods & keybind.mods) == keybind.mods) {
                keybind.press();
                if (keybind.axisValue == 0)
                    keybind.axisValue = 1f;
            }
        } else {
            if (keybind.polled)
                keybind.release();
        }
    }

    private static void processAxis(Keybind keybind, float value, float lastValue, float deadzone) {
        keybind.axisValue = value;
        keybind.lastAxisValue = lastValue;

        boolean pressed = Math.abs(value) > deadzone;
        if (!pressed || !keybind.isPressed())
            processKeybind(keybind, pressed, 0);
    }

    public static void releaseAll(boolean mouse, boolean keys) {
        for (Map.Entry<KeyType, List<Keybind>> entry : KEYBINDS.entrySet()) {
            KeyType key = entry.getKey();
            if (key == KeyType.MOUSE && !mouse)
                continue;
            if ((key == KeyType.KEY || key == KeyType.SCANCODE) && !keys)
                continue;

            for (Keybind keybind : entry.getValue())
                keybind.release();
        }

        if (mouse) firstMouse = true;
    }

    public static void flush() {
        for (List<Keybind> value : KEYBINDS.values()) {
            for (Keybind keybind : value) {
                //always reset delta-based inputs
                if (keybind.type == KeyType.MOUSE_MOVE || keybind.type == KeyType.MOUSE_SCROLL) {
                    keybind.release();
                }
                //release any keybinds that are no longer pressed
                else if (!keybind.pressed) {
                    keybind.release();
                }
                //update last axis value for any keybinds that are still pressed
                else {
                    keybind.lastAxisValue = keybind.axisValue;
                }
            }
        }
    }

    private void press() {
        clicks++;
        pressed = true;
        pollPressed = true;
    }

    private void release() {
        clicks = 0;
        pressed = false;
        pollPressed = false;
        lastAxisValue = axisValue = 0;
        polled = false;
    }

    public boolean click() {
        if (clicks > 0) {
            clicks--;
            return true;
        }

        return false;
    }

    public void polled() {
        polled = true;
    }

    public int getClicks() {
        return clicks;
    }

    public boolean isPressed() {
        return pollPressed;
    }

    public boolean isActuallyPressed() {
        return pressed;
    }

    public void set(int key, int mods, KeyType type, int joystick) {
        if (type != this.type) {
            KEYBINDS.get(this.type).remove(this);
            KEYBINDS.get(type).add(this);
        }

        this.key = key;
        this.mods = mods;
        this.type = type;
        this.joystick = joystick;
        this.text = null;
    }

    public String getName() {
        return name;
    }

    public Text getKeyText() {
        return text != null ? text : (text = type.getKeyText(key, mods));
    }

    public int getKey() {
        return key;
    }

    public int getMods() {
        return mods;
    }

    public KeyType getType() {
        return type;
    }

    public int getJoystick() {
        return joystick;
    }

    public float getAxisValue() {
        return axisValue;
    }

    public float getLastAxisValue() {
        return lastAxisValue;
    }

    public enum KeyType {
        KEY(key -> {
            String glfwName = glfwGetKeyName(key, glfwGetKeyScancode(key));
            return glfwName != null ? Text.of(glfwName.toUpperCase()) : Text.translated("key.keyboard." + key);
        }),
        SCANCODE(scancode -> {
            String glfwName = glfwGetKeyName(-1, scancode);
            return glfwName != null ? Text.of(glfwName.toUpperCase()) : Text.translated("key.scancode", scancode + 1);
        }),
        MOUSE(button -> button < 3 ? Text.translated("key.mouse." + button) : Text.translated("key.mouse", button + 1)),
        MOUSE_MOVE(axis -> Text.translated("key.mouse.move." + (axis == 0 ? "x" : "y"))),
        MOUSE_SCROLL(axis -> Text.translated("key.mouse.scroll." + axis)),
        GAMEPAD_BUTTON(button -> Text.translated("key.gamepad.button." + button)),
        GAMEPAD_AXIS(axis -> Text.translated("key.gamepad.axis." + axis)),
        XR_BUTTON(button -> Text.translated("key.xr.button",button + 1)),
        XR_TRIGGER(axis -> Text.translated("key.xr.trigger", axis + 1)),
        XR_JOYSTICK(axis -> Text.translated("key.xr.joystick." + (axis == 0 ? "x" : "y"))),
        JOYSTICK_BUTTON(button -> Text.translated("key.joystick.button", button + 1)),
        JOYSTICK_AXIS(axis -> Text.translated("key.joystick.axis", axis + 1)),
        JOYSTICK_HAT(hat -> {
            int hatId = hat >> 4;
            int dir = hat & 0xF;
            return Text.translated("key.joystick.hat." + dir, hatId + 1);
        });

        private final Function<Integer, Text> textFunction;

        KeyType(Function<Integer, Text> textFunction) {
            this.textFunction = textFunction;
        }

        public Text getKeyText(int key, int mods) {
            if (key == -1)
                return Text.translated("key.none");

            Text text = Text.empty();
            if (mods != 0) {
                if ((mods & GLFW_MOD_CONTROL) != 0)
                    text.append("Ctrl + ");
                if ((mods & GLFW_MOD_SHIFT) != 0)
                    text.append("Shift + ");
                if ((mods & GLFW_MOD_ALT) != 0)
                    text.append("Alt + ");
            }
            text.append(textFunction.apply(key));
            return text;
        }
    }
}
