package mayo.options;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import mayo.model.ModelRegistry;
import mayo.utils.IOUtils;
import mayo.world.AIBehaviour;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class Options {

    //actual options
    public int fov = 70;
    public float sensibility = 0.27f;

    //player model
    public ModelRegistry.Living player = ModelRegistry.Living.STRAWBERRY;

    //spawn rates
    public int enemySpawn = 60;
    public int healthSpawn = 300;
    public int boostSpawn = 300;

    //enemy behaviour
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    public AIBehaviour[] enemyBehaviour = {
            AIBehaviour.WALK
    };

    private static final Path OPTIONS_FILE = Path.of("./options.json");
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
        byte[] bytes = IOUtils.readFileBytes(OPTIONS_FILE);
        if (bytes == null)
            return new Options().save();

        //attempt to read file
        try {
            String options = new String(bytes, StandardCharsets.UTF_8);
            return MAPPER.readValue(options, Options.class).save();
        } catch (Exception e) {
            e.printStackTrace();
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
            e.printStackTrace();
        }
        return this;
    }
}
