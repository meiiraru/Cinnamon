package cinnamon.parsers;

import cinnamon.math.Transform;
import cinnamon.utils.IOUtils;
import cinnamon.utils.Resource;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.joml.Math;
import org.joml.Vector3f;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.function.BiConsumer;

import static cinnamon.events.Events.LOGGER;

public class BBModelTerrainLoader {

    public static void load(Resource res, BiConsumer<String, Transform> terrainConsumer) throws Exception {
        load(res, BBModelLoader.SCALE, terrainConsumer);
    }

    public static void load(Resource res, float scale, BiConsumer<String, Transform> terrainConsumer) throws Exception {
        LOGGER.debug("Loading bbmodel \"%s\"", res);

        InputStream stream = IOUtils.getResource(res);
        if (stream == null)
            throw new RuntimeException("Resource not found: " + res);

        try (stream; InputStreamReader reader = new InputStreamReader(stream)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();

            //parse meta
            BBModelLoader.checkVersion(json);

            //parse elements
            Transform transform = new Transform();
            JsonArray elements = json.getAsJsonArray("elements");

            for (JsonElement elementEl : elements) {
                JsonObject element = elementEl.getAsJsonObject();

                String id = element.get("name").getAsString();

                Vector3f pos = parseVec3(element.getAsJsonArray("origin"));
                pos.mul(scale);
                transform.setPos(pos);

                if (element.has("rotation")) {
                    Vector3f rot = parseVec3(element.getAsJsonArray("rotation"));
                    transform.getRot().rotationXYZ(Math.toRadians(rot.x), Math.toRadians(rot.y), Math.toRadians(rot.z));
                    transform.markDirty();
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
