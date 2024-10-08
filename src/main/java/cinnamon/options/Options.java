package cinnamon.options;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import cinnamon.registry.LivingModelRegistry;
import cinnamon.utils.IOUtils;
import cinnamon.world.AIBehaviour;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static cinnamon.Client.LOGGER;

public class Options {

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
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    public AIBehaviour[] enemyBehaviour = {
            AIBehaviour.WALK
    };

    private static final Path OPTIONS_FILE = IOUtils.ROOT_FOLDER.resolve("options.json");
    private static final ObjectMapper MAPPER = JsonMapper
            .builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL)
            .build();

    private Options() {}

    public static Options load() {
        //file do not exist - create new
        if (!Files.exists(OPTIONS_FILE))
            return new Options().save();

        //could not read file - create new
        byte[] bytes = IOUtils.readFile(OPTIONS_FILE);
        if (bytes == null)
            return new Options().save();

        //attempt to read file
        try {
            String options = new String(bytes, StandardCharsets.UTF_8);
            return MAPPER.readValue(options, Options.class).save();
        } catch (Exception e) {
            LOGGER.error("Failed to read options file", e);
        }

        //...it failed - create new
        return new Options().save();
    }

    public Options save() {
        //save config :)
        try {
            String json = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(this);
            IOUtils.writeFile(OPTIONS_FILE, json.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            LOGGER.error("Failed to save options file", e);
        }
        return this;
    }
}
