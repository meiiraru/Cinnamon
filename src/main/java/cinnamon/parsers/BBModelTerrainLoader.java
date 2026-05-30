package cinnamon.parsers;

import cinnamon.utils.IOUtils;
import cinnamon.utils.Resource;
import cinnamon.utils.Version;
import cinnamon.world.Transform;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.joml.Vector3f;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.function.BiConsumer;

import static cinnamon.events.Events.LOGGER;

public class BBModelTerrainLoader {

    public static final Version SUPPORTED_VERSION = new Version("5.0.0");
    public static final float SCALE = 1 / 16f;

    public static void load(Resource res, BiConsumer<String, Transform> terrainConsumer) throws Exception {
        LOGGER.debug("Loading bbmodel \"%s\"", res);

        InputStream stream = IOUtils.getResource(res);
        if (stream == null)
            throw new RuntimeException("Resource not found: " + res);

        try (stream; InputStreamReader reader = new InputStreamReader(stream)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();

            //parse meta
            JsonObject meta = json.getAsJsonObject("meta");
            String version = meta.get("format_version").getAsString();
            Version ver = new Version(version);
            if (SUPPORTED_VERSION.compareTo(ver) > 0)
                throw new RuntimeException("Unsupported bbmodel version: " + version);

            //parse elements
            Transform transform = new Transform();
            JsonArray elements = json.getAsJsonArray("elements");

            for (JsonElement elementEl : elements) {
                JsonObject element = elementEl.getAsJsonObject();

                String id = element.get("name").getAsString();

                Vector3f pos = parseVec3(element.getAsJsonArray("origin"));
                pos.mul(SCALE);
                transform.setPos(pos);

                if (element.has("rotation")) {
                    Vector3f rot = parseVec3(element.getAsJsonArray("rotation"));
                    transform.setRot(rot);
                }

                terrainConsumer.accept(id, transform);
                transform.identity();
            }
        }
    }

    public static Vector3f parseVec3(JsonArray array) {
        return new Vector3f(array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat());
    }
}
