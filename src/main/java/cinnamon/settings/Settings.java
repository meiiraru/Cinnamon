package cinnamon.settings;

import cinnamon.Client;
import cinnamon.registry.LivingModelRegistry;
import cinnamon.sound.SoundCategory;
import cinnamon.utils.IOUtils;
import com.google.gson.*;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static cinnamon.Client.LOGGER;

public class Settings {

    //settings registry
    static final List<Setting<?>> SETTINGS = new ArrayList<>();


    // -- settings -- //


    private static final int VERSION = 1;

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

    static {
        //player name do not have a getter
        Setting.Strings player = new Setting.Strings("player.playername", "Player");
        player.setListener(v -> Client.getInstance().setName(v));

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
                    LOGGER.info("Updating settings file from version {} to {}", v, VERSION);
                } else {
                    LOGGER.warn("Unknown settings version {}, forcing update with default missing values", v);
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
                LOGGER.warn("Failed to load setting \"{}\", using default value \"{}\"", name, setting.getDefault(), e);
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
