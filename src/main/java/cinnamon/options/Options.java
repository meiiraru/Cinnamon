package cinnamon.options;

import cinnamon.Client;
import cinnamon.registry.LivingModelRegistry;
import cinnamon.utils.IOUtils;
import cinnamon.world.AIBehaviour;
import com.google.gson.*;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Options {

    private static final Path OPTIONS_FILE = IOUtils.ROOT_FOLDER.resolve("options.json");
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

    //actual options
    public int fov = 70;
    public float sensibility = 0.27f;
    public float guiScale = 3f;

    //player model
    public LivingModelRegistry player = LivingModelRegistry.STRAWBERRY;

    //spawn rates
    public int enemySpawn = 60;
    public int healthSpawn = 300;
    public int boostSpawn = 300;

    //enemy behaviour
    public List<AIBehaviour> enemyBehaviour = List.of(
            AIBehaviour.WALK
    );

    private Options() {}

    public static Options load() {
        Options options = new Options();

        byte[] bytes = IOUtils.readFile(OPTIONS_FILE);
        if (bytes == null)
            return options.save();

        JsonObject json = JsonParser.parseString(new String(bytes)).getAsJsonObject();
        try {
            options.fov = json.get("fov").getAsInt();
            options.sensibility = json.get("sensibility").getAsFloat();
            options.guiScale = json.get("guiScale").getAsFloat();

            options.player = LivingModelRegistry.valueOf(json.get("player").getAsString().toUpperCase());

            options.enemySpawn = json.get("enemySpawn").getAsInt();
            options.healthSpawn = json.get("healthSpawn").getAsInt();
            options.boostSpawn = json.get("boostSpawn").getAsInt();

            List<AIBehaviour> enemyBehaviour = new ArrayList<>();
            for (JsonElement element : json.getAsJsonArray("enemyBehaviour"))
                enemyBehaviour.add(AIBehaviour.valueOf(element.getAsString().toUpperCase()));
            options.enemyBehaviour = enemyBehaviour;
        } catch (Exception e) {
            Client.LOGGER.error("Failed to load saved options", e);
        }

        return options.save();
    }

    public Options save() {
        JsonObject json = new JsonObject();

        json.addProperty("fov", fov);
        json.addProperty("sensibility", sensibility);
        json.addProperty("guiScale", guiScale);

        json.addProperty("player", player.name());

        json.addProperty("enemySpawn", enemySpawn);
        json.addProperty("healthSpawn", healthSpawn);
        json.addProperty("boostSpawn", boostSpawn);

        JsonArray enemyBehaviour = new JsonArray();
        for (AIBehaviour behaviour : this.enemyBehaviour)
            enemyBehaviour.add(behaviour.name());
        json.add("enemyBehaviour", enemyBehaviour);

        IOUtils.writeFile(OPTIONS_FILE, GSON.toJson(json).getBytes());
        return this;
    }
}
