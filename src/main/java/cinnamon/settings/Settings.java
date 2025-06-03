package cinnamon.settings;

import cinnamon.Client;
import cinnamon.input.InputManager;
import cinnamon.lang.LangManager;
import cinnamon.registry.LivingModelRegistry;
import cinnamon.sound.SoundCategory;
import cinnamon.utils.IOUtils;
import cinnamon.vr.XrInput;
import com.google.gson.*;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static cinnamon.Client.LOGGER;
import static cinnamon.input.Keybind.KeyType.*;
import static org.lwjgl.glfw.GLFW.*;

public class Settings {

    //settings registry
    static final List<Setting<?>> SETTINGS = new ArrayList<>();


    // -- settings -- //


    private static final int VERSION = 1;

    //lang
    public static final Setting.Strings lang = new Setting.Strings("lang.lang", LangManager.MAIN_LANG);

    //screen
    public static final Setting.Ints fov = new Setting.Ints("screen.fov", 70);
    public static final Setting.Floats guiScale = new Setting.Floats("screen.guiScale", 0f);
    public static final Setting.Bools showFPS = new Setting.Bools("screen.show_fps", false);

    //mouse
    public static final Setting.Floats sensibility = new Setting.Floats("mouse.sensibility", 0.27f);
    public static final Setting.Bools invertX = new Setting.Bools("mouse.invert_mouse_x", false);
    public static final Setting.Bools invertY = new Setting.Bools("mouse.invert_mouse_y", false);

    //player
    public static final Setting.Enums<LivingModelRegistry> playermodel = new Setting.Enums<>("player.player_model", LivingModelRegistry.STRAWBERRY);

    //sound device
    public static final Setting.Strings soundDevice = new Setting.Strings("sound.device", "");

    //xr
    public static final Setting.Strings xrInteractionProfile = new Setting.Strings("xr.interaction_profile", XrInput.DEFAULT_PROFILE.toString());
    public static final Setting.Bools
            xrHapticFeedback = new Setting.Bools("xr.haptic_feedback", true),
            xrSnapTurn       = new Setting.Bools("xr.snap_turn", false);
    public static final Setting.Floats
            xrTurningAngle = new Setting.Floats("xr.turning_angle", 3f),
            xrSnapTurningAngle = new Setting.Floats("xr.snap_turning_angle", 30f);

    //keybinds
    public static final Setting.Keybind
            //movement
            forward = new Setting.Keybind("keybind.forward", GLFW_KEY_W, KEY),
            backward = new Setting.Keybind("keybind.backward", GLFW_KEY_S, KEY),
            left = new Setting.Keybind("keybind.left", GLFW_KEY_A, KEY),
            right = new Setting.Keybind("keybind.right", GLFW_KEY_D, KEY),

            jump = new Setting.Keybind("keybind.jump", GLFW_KEY_SPACE, KEY),
            sneak = new Setting.Keybind("keybind.sneak", GLFW_KEY_LEFT_SHIFT, KEY),
            sprint = new Setting.Keybind("keybind.sprint", GLFW_KEY_TAB, KEY),

            //item
            attack = new Setting.Keybind("keybind.attack", GLFW_MOUSE_BUTTON_1, MOUSE),
            use = new Setting.Keybind("keybind.use", GLFW_MOUSE_BUTTON_2, MOUSE),
            pick = new Setting.Keybind("keybind.pick", GLFW_MOUSE_BUTTON_3, MOUSE);

    static {
        //player name do not have a getter
        Setting.Strings player = new Setting.Strings("player.playername", Client.getInstance().name);
        player.setListener(v -> Client.getInstance().setName(v));

        //raw mouse
        Setting.Bools rawMouse = new Setting.Bools("mouse.raw_mouse", true);
        rawMouse.setListener(InputManager::setRawMouseInput);

        //wrapper for sound categories
        for (SoundCategory sound : SoundCategory.values()) {
            Setting.Ranges setting = new Setting.Ranges("sound." + sound.name().toLowerCase(), 1f, 0f, 1f) {
                @Override
                public Float get() {
                    return sound.getVolume();
                }
            };
            setting.setListener(sound::setVolume);
        }
    }

    //list index = version number
    //map = setting -> old version name
    private static final List<Map<Setting<?>, String>> VERSION_MAP = List.of();


    // -- IO -- //


    private static final Path OPTIONS_FILE = IOUtils.ROOT_FOLDER.resolve("settings.json");
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

    public static void load() {
        JsonObject json;

        //read the settings file
        try {
            byte[] bytes = IOUtils.readFile(OPTIONS_FILE);
            if (bytes == null) {
                save();
                return;
            }
            json = JsonParser.parseString(new String(bytes)).getAsJsonObject();
        } catch (Exception e) {
            LOGGER.error("Failed to load saved settings", e);
            save();
            return;
        }

        LOGGER.info("Loading settings file...");

        //versioning
        Map<Setting<?>, String> versionMap;
        try {
            int v = json.get("_version").getAsInt();
            if (v != VERSION) {
                if (v > 0 && v <= VERSION_MAP.size()) {
                    versionMap = VERSION_MAP.get(v - 1);
                    LOGGER.info("Updating settings file from version %s to %s", v, VERSION);
                } else {
                    LOGGER.warn("Unknown settings version %s, forcing update with default missing values", v);
                    versionMap = null;
                }
            } else {
                versionMap = null;
            }
        } catch (Exception e) {
            LOGGER.error("Failed to update settings file", e);
            save();
            return;
        }

        //load settings
        for (Setting<?> setting : SETTINGS) {
            String name = setting.getName();
            String[] split = (versionMap != null && versionMap.containsKey(setting) ? versionMap.get(setting) : name).split("\\.", -1);

            try {
                //get the value
                JsonObject obj = json;
                int i = 0;
                for (; i < split.length - 1; i++) {
                    String s = split[i];

                    //check for object
                    JsonElement e = obj.get(s);
                    if (e == null) { //setting was not saved
                        obj = null;
                        break;
                    }
                    if (!e.isJsonObject()) //setting conflict
                        break;

                    obj = e.getAsJsonObject();
                }

                //setting not saved
                if (obj == null)
                    continue;

                //get the remaining path as the setting name
                String path = String.join(".", Arrays.copyOfRange(split, i, split.length));

                //set the loaded value
                JsonElement value = obj.get(path);
                if (value != null)
                    setting.fromJson(value);
            } catch (Exception e) {
                LOGGER.warn("Failed to load setting \"%s\", using default value \"%s\"", name, setting.getDefault(), e);
            }
        }

        save();
    }

    public static void save() {
        LOGGER.info("Saving settings file...");
        JsonObject json = new JsonObject();

        //versioning
        json.addProperty("_version", VERSION);

        //save settings
        for (Setting<?> setting : SETTINGS) {
            String name = setting.getName();
            String[] split = name.split("\\.", -1);

            //create the path
            JsonObject obj = json;
            int i = 0;
            for (; i < split.length - 1; i++) {
                String s = split[i];

                //add the object
                if (!obj.has(s))
                    obj.add(s, new JsonObject());

                //check for object
                JsonElement e = obj.get(s);
                if (!e.isJsonObject())
                    break;

                obj = e.getAsJsonObject();
            }

            //get the remaining path as the setting name
            String path = String.join(".", Arrays.copyOfRange(split, i, split.length));
            obj.add(path, setting.toJson());
        }

        //write to file
        try {
            IOUtils.writeFile(OPTIONS_FILE, GSON.toJson(json).getBytes());
        } catch (Exception e) {
            LOGGER.error("Failed to save settings", e);
        }
    }
}
