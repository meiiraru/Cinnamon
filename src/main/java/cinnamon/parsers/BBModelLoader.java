package cinnamon.parsers;

import cinnamon.animation.Animation;
import cinnamon.animation.Bone;
import cinnamon.model.obj.Mesh;
import cinnamon.utils.IOUtils;
import cinnamon.utils.Resource;
import cinnamon.utils.Version;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import static cinnamon.events.Events.LOGGER;

public class BBModelLoader {

    public static final Version MIN_SUPPORTED_VERSION = new Version("5.0.0");
    public static final float SCALE = 1 / 16f;

    public static BBModelData load(Resource res) throws Exception {
        LOGGER.debug("Loading bbmodel \"%s\"", res);

        InputStream stream = IOUtils.getResource(res);
        if (stream == null)
            throw new RuntimeException("Resource not found: " + res);

        try (stream; InputStreamReader reader = new InputStreamReader(stream)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();

            //parse meta
            checkVersion(json);
        }

        return null;
    }

    public static void checkVersion(JsonObject root) {
        JsonObject meta = root.has("meta") ? root.getAsJsonObject("meta") : null;
        if (meta == null || !meta.has("format_version"))
            throw new RuntimeException("Invalid bbmodel file: missing meta.format_version");

        String version = meta.get("format_version").getAsString();

        //make version match the semver pattern
        String[] parts = version.split("\\.");
        if (parts.length < 3)
            version += ".0";

        Version ver = new Version(version);
        if (MIN_SUPPORTED_VERSION.compareTo(ver) > 0)
            throw new RuntimeException("Unsupported bbmodel version: " + version);
    }

    public record BBModelData(Mesh mesh, Bone rootBone, List<Animation> animations) {}
}
