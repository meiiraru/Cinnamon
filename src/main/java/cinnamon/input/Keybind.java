package cinnamon.input;

import cinnamon.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.lwjgl.glfw.GLFW.*;

public class Keybind {

    private static final Map<KeyType, List<Keybind>> KEYBINDS = Map.of(
            KeyType.KEY, new ArrayList<>(),
            KeyType.SCANCODE, new ArrayList<>(),
            KeyType.MOUSE, new ArrayList<>()
    );

    private final String name;
    private final int defaultKey, defaultMods;
    private final KeyType defaultType;

    private int key, mods;
    private KeyType type;

    private boolean pressed;
    private int clicks;

    private Text text;

    public Keybind(String name, int key, KeyType type) {
        this(name, key, 0, type);
    }

    public Keybind(String name, int key, int mods, KeyType type) {
        this.name = name;
        this.defaultKey = this.key = key;
        this.defaultMods = this.mods = mods;
        this.defaultType = this.type = type;
        updateText();
        KEYBINDS.get(defaultType).add(this);
    }

    public static void mousePress(int button, int action, int mods) {
        if (button == -1)
            return;

        for (Keybind keybind : KEYBINDS.get(KeyType.MOUSE)) {
            if (keybind.key == button) {
                if (action != GLFW_RELEASE && (keybind.mods == 0 || mods == keybind.mods))
                    keybind.press();
                else
                    keybind.release();
            }
        }
    }

    public static void keyPress(int key, int scancode, int action, int mods) {
        if (key == -1 && scancode == -1)
            return;

        if (key != -1) {
            for (Keybind keybind : KEYBINDS.get(KeyType.KEY)) {
                if (keybind.key == key) {
                    if (action != GLFW_RELEASE && (keybind.mods == 0 || mods == keybind.mods))
                        keybind.press();
                    else
                        keybind.release();
                }
            }
        } else {
            for (Keybind keybind : KEYBINDS.get(KeyType.SCANCODE)) {
                if (keybind.key == scancode) {
                    if (action != GLFW_RELEASE && (keybind.mods == 0 || mods == keybind.mods))
                        keybind.press();
                    else
                        keybind.release();
                }
            }
        }
    }

    public static void releaseAll() {
        for (List<Keybind> keybinds : KEYBINDS.values())
            for (Keybind keybind : keybinds)
                keybind.release();
    }

    private void press() {
        clicks++;
        pressed = true;
    }

    private void release() {
        clicks = 0;
        pressed = false;
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

    public boolean isPressed() {
        return pressed;
    }

    public void set(int key, int mods, KeyType type) {
        if (type != this.type) {
            KEYBINDS.get(this.type).remove(this);
            KEYBINDS.get(type).add(this);
        }

        this.key = key;
        this.mods = mods;
        this.type = type;
        updateText();
    }

    public boolean isDefault() {
        return key == defaultKey && mods == defaultMods && type == defaultType;
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

    public enum KeyType {
        KEY(key -> {
            String glfwName = glfwGetKeyName(key, -1);
            return glfwName != null ? Text.of(glfwName.toUpperCase()) : Text.translated("key.keyboard", key);
        }),
        SCANCODE(scancode -> {
            String glfwName = glfwGetKeyName(-1, scancode);
            return glfwName != null ? Text.of(glfwName.toUpperCase()) : Text.translated("key.scancode", scancode);
        }),
        MOUSE(button -> Text.translated("key.mouse", button + 1));

        private final Function<Integer, Text> textFunction;

        KeyType(Function<Integer, Text> textFunction) {
            this.textFunction = textFunction;
        }
    }
}
