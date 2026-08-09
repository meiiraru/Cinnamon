package cinnamon.settings;

import cinnamon.Client;
import cinnamon.gui.GUISkin;
import cinnamon.input.InputManager;
import cinnamon.lang.LangManager;
import cinnamon.registry.LivingModelRegistry;
import cinnamon.sound.SoundCategory;
import cinnamon.sound.SoundManager;
import cinnamon.utils.IOUtils;
import cinnamon.utils.Pair;
import cinnamon.utils.Resource;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
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
    public static final List<Setting<?>> SETTINGS = new ArrayList<>();


    // -- settings -- //


    private static final int VERSION = 1;

    // -- video -- //

    //lang
    public static final Setting.List lang = new Setting.List("video.lang.lang", LangManager.MAIN_LANG, () -> {
        List<Pair<String, String>> list = new ArrayList<>();
        for (Map.Entry<String, String> entry : LangManager.getLangList().entrySet())
            list.add(new Pair<>(entry.getKey(), entry.getValue()));
        return list;
    });

    //display
    public static final Setting.Bools
            showFPS         = new Setting.Bools("video.display.show_fps", false),
            vsync           = new Setting.Bools("video.display.vsync", false),
            dynamicFpsLimit = new Setting.Bools("video.display.dynamic_fps_limit", true);
    public static final Setting.Floats
            guiScale = new Setting.Floats("video.display.gui_scale", 0f);
    public static final Setting.Ints
            fov      = new Setting.Ints("video.display.fov", 70),
            fpsLimit = new Setting.Ints("video.display.fps_limit", 0);
    public static final Setting.List
            guiSkin = new Setting.List("video.display.gui_skin", "", () -> {
                List<Pair<String, String>> list = new ArrayList<>();
                for (Map.Entry<String, String> entry : GUISkin.getThemes().entrySet())
                    list.add(new Pair<>(entry.getKey(), entry.getValue()));
                return list;
            });

    //graphics
    public static final Setting.Bools
            fxaa      = new Setting.Bools("video.graphics.fxaa", true),
            lensFlare = new Setting.Bools("video.graphics.lens_flare", true);
    public static final Setting.IntRanges
            volumetricLights = new Setting.IntRanges("video.graphics.volumetric_lights", 3, -1, 4),
            ssaoLevel        = new Setting.IntRanges("video.graphics.ssao_level", 3, -1, 4),
            ssrLevel         = new Setting.IntRanges("video.graphics.ssr_level", 3, -1, 4),
            shadowQuality    = new Setting.IntRanges("video.graphics.shadow_quality", 3, -1, 4);
    public static final Setting.Ranges
            renderScale   = new Setting.Ranges("video.graphics.render_scale", 1f, 0.01f, 4f),
            bloomStrength = new Setting.Ranges("video.graphics.bloom_strength", 1f, 0f, 5f);

    // -- sounds -- //

    //sound device
    public static final Setting.List soundDevice = new Setting.List("sound.device", "", () -> {
        List<Pair<String, String>> list = new ArrayList<>();
        list.add(new Pair<>("", "gui.default"));
        for (String device : SoundManager.getDevices())
            list.add(new Pair<>(device, device.replaceFirst("^OpenAL Soft on ", "")));
        return list;
    });

    //categories added in static loop

    // -- accessibility -- //

    //general
    public static final Setting.Ints
            doubleKeypressTime = new Setting.Ints("accessibility.general.double_keypress_time", 10),
            doubleClickTime    = new Setting.Ints("accessibility.general.double_click_time", 10),
            cursorBlinkDelay   = new Setting.Ints("accessibility.general.cursor_blink_delay", 20);
    public static final Setting.Floats viewBobbingStrength  = new Setting.Floats("accessibility.general.view_bobbing_strength", 1f);
    public static final Setting.Bools actionWheelRunOnClose = new Setting.Bools("accessibility.general.action_wheel_run_on_close", false);
    public static final Setting.Ranges gamepadDeadzone      = new Setting.Ranges("accessibility.general.gamepad_deadzone", 0.33f, 0f, 1f);

    //xr
    public static final Setting.Bools
            xrHapticFeedback = new Setting.Bools("accessibility.xr.haptic_feedback", true),
            xrSnapTurn       = new Setting.Bools("accessibility.xr.snap_turn", true),
            xrClickOnHover   = new Setting.Bools("accessibility.xr.click_on_hover", true);
    public static final Setting.Floats
            xrTurningAngle     = new Setting.Floats("accessibility.xr.turning_angle", 3f),
            xrSnapTurningAngle = new Setting.Floats("accessibility.xr.snap_turning_angle", 30f);
    public static final Setting.Ints
            xrClickOnHoverDelay = new Setting.Ints("accessibility.xr.click_on_hover_delay", 30);

    // -- controls -- //

    //mouse
    public static final Setting.Floats sensibility = new Setting.Floats("controls.mouse.sensibility", 0.5f);
    public static final Setting.Bools
            invertX  = new Setting.Bools("controls.mouse.invert_mouse_x", false),
            invertY  = new Setting.Bools("controls.mouse.invert_mouse_y", false),
            rawMouse = new Setting.Bools("controls.mouse.raw_mouse", true);

    //keybinds
    public static final Setting.Keybind
            //movement
            forward  = new Setting.Keybind("controls.keybind.movement.forward", GLFW_KEY_W, KEY),
            backward = new Setting.Keybind("controls.keybind.movement.backward", GLFW_KEY_S, KEY),
            left     = new Setting.Keybind("controls.keybind.movement.left", GLFW_KEY_A, KEY),
            right    = new Setting.Keybind("controls.keybind.movement.right", GLFW_KEY_D, KEY),

            jump   = new Setting.Keybind("controls.keybind.movement.jump", GLFW_KEY_SPACE, KEY),
            sneak  = new Setting.Keybind("controls.keybind.movement.sneak", GLFW_KEY_LEFT_CONTROL, KEY),
            sprint = new Setting.Keybind("controls.keybind.movement.sprint", GLFW_KEY_LEFT_SHIFT, KEY),

            //item
            attack = new Setting.Keybind("controls.keybind.item.attack", GLFW_MOUSE_BUTTON_1, MOUSE),
            use    = new Setting.Keybind("controls.keybind.item.use", GLFW_MOUSE_BUTTON_2, MOUSE),
            pick   = new Setting.Keybind("controls.keybind.item.pick", GLFW_MOUSE_BUTTON_3, MOUSE),
            drop   = new Setting.Keybind("controls.keybind.item.drop", GLFW_KEY_Q, KEY),
            reload = new Setting.Keybind("controls.keybind.item.reload", GLFW_KEY_R, KEY),
            inv1   = new Setting.Keybind("controls.keybind.inv.inv1", GLFW_KEY_1, KEY),
            inv2   = new Setting.Keybind("controls.keybind.inv.inv2", GLFW_KEY_2, KEY),
            inv3   = new Setting.Keybind("controls.keybind.inv.inv3", GLFW_KEY_3, KEY),
            inv4   = new Setting.Keybind("controls.keybind.inv.inv4", GLFW_KEY_4, KEY),
            inv5   = new Setting.Keybind("controls.keybind.inv.inv5", GLFW_KEY_5, KEY),
            inv6   = new Setting.Keybind("controls.keybind.inv.inv6", GLFW_KEY_6, KEY),
            inv7   = new Setting.Keybind("controls.keybind.inv.inv7", GLFW_KEY_7, KEY),
            inv8   = new Setting.Keybind("controls.keybind.inv.inv8", GLFW_KEY_8, KEY),
            inv9   = new Setting.Keybind("controls.keybind.inv.inv9", GLFW_KEY_9, KEY),

            //vehicle
            honk   = new Setting.Keybind("controls.keybind.car.honk", GLFW_KEY_F, KEY),
            lights = new Setting.Keybind("controls.keybind.car.lights", GLFW_KEY_H, KEY),
            brake  = new Setting.Keybind("controls.keybind.car.brake", GLFW_KEY_SPACE, KEY);

    // -- misc -- //

    //player
    public static final Setting.Enums playerModel = new Setting.Enums("misc.player.player_model", LivingModelRegistry.STRAWBERRY.name(), LivingModelRegistry.class);
    public static final Setting.Strings playerName = new Setting.Strings("misc.player.player_name", "");

    static {
        //display
        lang.setListener(str -> LangManager.loadForLang(str.isBlank() ? null : str));
        vsync.setListener(v -> Client.getInstance().window.toggleVsync(v));
        guiSkin.setListener(str -> GUISkin.setCurrentSkin(str.isBlank() ? null : new Resource(str)));
        guiScale.setListener(f -> {
            Client c = Client.getInstance();
            if (c.isInitialized())
                c.windowResize(c.window.width, c.window.height);
        });
        fov.setListener(f -> {
            Client c = Client.getInstance();
            if (c.isInitialized())
                c.windowResize(c.window.width, c.window.height);
        });

        //sound
        soundDevice.setListener(str -> {
            if (SoundManager.isInitialized())
                SoundManager.swapDevice(str);
        });

        //raw mouse
        rawMouse.setListener(InputManager::setRawMouseInput);

        //wrapper for sound categories
        for (SoundCategory sound : SoundCategory.values()) {
            Setting.Ranges setting = new Setting.Ranges("sound.volume." + sound.name().toLowerCase(), sound == SoundCategory.MASTER ? 0.5f : 1f, 0f, 1f) {
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
        Resource options = new Resource("", OPTIONS_FILE.toString());
        if (!IOUtils.hasResource(options)) {
            save();
            return;
        }

        //read the settings file
        LOGGER.info("Loading settings file...");

        JsonObject json;
        try (InputStream stream = IOUtils.getResource(options); InputStreamReader reader = new InputStreamReader(stream)) {
            json = JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception e) {
            LOGGER.error("Failed to load saved settings", e);
            save();
            return;
        }

        //versioning
        Map<Setting<?>, String> versionMap;
        try {
            int v = json.get("__version").getAsInt();
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
        json.addProperty("__version", VERSION);

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
