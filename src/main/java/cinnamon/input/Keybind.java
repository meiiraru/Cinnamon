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

    private final String name;
    private final int defaultKey, defaultMods;
    private final KeyType defaultType;

    private int key, mods;
    private KeyType type;

    private Text text;

    //generic button
    private boolean pressed;
    private boolean pollPressed;
    private int clicks;

    //joystick exclusive
    private final int defaultJoystick;
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
        this.defaultKey = this.key = key;
        this.defaultMods = this.mods = mods;
        this.defaultType = this.type = type;
        this.defaultJoystick = this.joystick = joystick;
        updateText();
        KEYBINDS.get(defaultType).add(this);
        LOGGER.debug("Registered %s keybind: %s (%s)", type.name(), name, text.asString());
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

    public static void gamepadButtonPress(int button, boolean pressed, int joystick) {
        for (Keybind keybind : KEYBINDS.get(KeyType.GAMEPAD_BUTTON)) {
            if (keybind.key == button && keybind.joystick == joystick)
                processKeybind(keybind, pressed, 0);
        }
    }

    public static void gamepadAxisMove(int axis, float value, int joystick, float lastValue) {
        float deadzone = Settings.gamepadDeadzone.get();
        for (Keybind keybind : KEYBINDS.get(KeyType.GAMEPAD_AXIS)) {
            if (keybind.key == axis && keybind.joystick == joystick) {
                keybind.axisValue = value;
                keybind.lastAxisValue = lastValue;

                boolean pressed = Math.abs(value) > deadzone;
                processKeybind(keybind, pressed, 0);
            }
        }
    }


    private static void processKeybind(Keybind keybind, boolean pressed, int mods) {
        keybind.pressed = pressed;
        if (pressed && (keybind.mods == 0 || mods == keybind.mods))
            keybind.press();
    }

    public static void releaseAll(boolean mouse, boolean keys) {
        if (mouse) {
            for (Keybind keybind : KEYBINDS.get(KeyType.MOUSE))
                keybind.release();
        }
        if (keys) {
            for (Keybind keybind : KEYBINDS.get(KeyType.KEY))
                keybind.release();
            for (Keybind keybind : KEYBINDS.get(KeyType.SCANCODE))
                keybind.release();
        }
    }

    public static void flush() {
        for (List<Keybind> value : KEYBINDS.values()) {
            for (Keybind keybind : value) {
                if (!keybind.pressed)
                    keybind.release();
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
    }

    private void updateText() {
        if (key == -1) {
            text = Text.translated("key.none");
            return;
        }

        text = Text.empty();
        if (mods != 0) {
            if ((mods & GLFW_MOD_CONTROL) != 0)
                text.append("Ctrl + ");
            if ((mods & GLFW_MOD_SHIFT) != 0)
                text.append("Shift + ");
            if ((mods & GLFW_MOD_ALT) != 0)
                text.append("Alt + ");
        }
        text.append(type.textFunction.apply(key));
    }

    public boolean click() {
        if (clicks > 0) {
            clicks--;
            return true;
        }

        return false;
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
        updateText();
    }

    public boolean isDefault() {
        return key == defaultKey && mods == defaultMods && type == defaultType && joystick == defaultJoystick;
    }

    public String getName() {
        return name;
    }

    public Text getKeyText() {
        return text;
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
            return glfwName != null ? Text.of(glfwName.toUpperCase()) : Text.translated("key.scancode." + scancode);
        }),
        MOUSE(button -> button < 3 ? Text.translated("key.mouse." + (button + 1)) : Text.translated("key.mouse", button + 1)),
        GAMEPAD_BUTTON(button -> Text.translated("key.gamepad.button." + button + 1)),
        GAMEPAD_AXIS(axis -> Text.translated("key.gamepad.axis." + axis + 1));

        private final Function<Integer, Text> textFunction;

        KeyType(Function<Integer, Text> textFunction) {
            this.textFunction = textFunction;
        }
    }
}
