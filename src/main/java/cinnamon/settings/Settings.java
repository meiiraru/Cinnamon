package cinnamon.settings;

import cinnamon.Client;
import cinnamon.registry.LivingModelRegistry;
import cinnamon.sound.SoundCategory;
import cinnamon.utils.IOUtils;
import cinnamon.utils.Pair;
import com.google.gson.*;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static cinnamon.Client.LOGGER;

public class Settings {

    //settings registry
    static final List<Setting<?>> SETTINGS = new ArrayList<>();

    private Settings() {
        //nope!
    }


    // -- settings -- //


    private static final int VERSION = 1;

    //screen
    public final Setting.SInt fov = new Setting.SInt("screen", "fov", 70);
    public final Setting.SFloat guiScale = new Setting.SFloat("screen", "guiScale", 0f);
    public final Setting.SBool showFPS = new Setting.SBool("screen", "show_fps", false);

    //mouse
    public final Setting.SFloat sensibility = new Setting.SFloat("mouse", "sensibility", 0.27f);
    public final Setting.SBool invertX = new Setting.SBool("mouse", "invert_mouse_x", false);
    public final Setting.SBool invertY = new Setting.SBool("mouse", "invert_mouse_y", false);

    //player
    public final Setting.SEnum<LivingModelRegistry> player = new Setting.SEnum<>("player", "player", LivingModelRegistry.STRAWBERRY);

    static {
        //player name do not have a getter
        new Setting.SString("player", "playername", "Player") {
            @Override
            public void set(String value) {
                super.set(value);
                Client.getInstance().setName(get());
            }
        };

        //wrapper for sound categories
        for (SoundCategory sound : SoundCategory.values()) {
            new Setting.SRange("sound", sound.name().toLowerCase(), 1f, 0f, 1f) {
                @Override
                public Float get() {
                    return sound.getVolume();
                }
                @Override
                public void set(Float value) {
                    super.set(value);
                    sound.setVolume(null, super.get());
                }
            };
        }
    }

    //list index = version number
    //Pair<new category, new name> <- Pair<old category, old name>
    private static final List<Map<Pair<String, String>, Pair<String, String>>> VERSION_MAP = List.of();


    // -- IO -- //


    private static final Path OPTIONS_FILE = IOUtils.ROOT_FOLDER.resolve("settings.json");
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

    public static Settings load() {
        Settings settings = new Settings();
        JsonObject json;

        //read the settings file
        try {
            byte[] bytes = IOUtils.readFile(OPTIONS_FILE);
            if (bytes == null)
                return settings.save();
            json = JsonParser.parseString(new String(bytes)).getAsJsonObject();
        } catch (Exception e) {
            LOGGER.error("Failed to load saved settings", e);
            return settings.save();
        }

        LOGGER.info("Loading settings file...");

        //versioning
        Map<Pair<String, String>, Pair<String, String>> versionMap;
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
            return settings.save();
        }

        //load settings
        for (Setting<?> setting : SETTINGS) {
            String category = setting.getCategory();
            String name = setting.getName();

            //get the old category and name
            if (versionMap != null) {
                Pair<String, String> pair = versionMap.get(Pair.of(category, name));
                if (pair != null) {
                    category = pair.first();
                    name = pair.second();
                }
            }

            if (!json.has(category))
                continue;

            try {
                JsonElement element = json.getAsJsonObject(category).get(name);
                if (element != null)
                    setting.fromJson(element);
            } catch (Exception e) {
                LOGGER.warn("Failed to load setting \"{}\" \"{}\", using default value \"{}\"", category, name, setting.getDefault(), e);
            }
        }

        return settings.save();
    }

    public Settings save() {
        LOGGER.info("Saving settings file...");
        JsonObject json = new JsonObject();

        //versioning
        json.addProperty("_version", VERSION);

        //save settings
        for (Setting<?> setting : SETTINGS) {
            String category = setting.getCategory();
            if (!json.has(category))
                json.add(category, new JsonObject());
            json.getAsJsonObject(category).add(setting.getName(), GSON.toJsonTree(setting.get()));
        }

        //write to file
        try {
            IOUtils.writeFile(OPTIONS_FILE, GSON.toJson(json).getBytes());
        } catch (Exception e) {
            LOGGER.error("Failed to save settings", e);
        }

        return this;
    }
}
