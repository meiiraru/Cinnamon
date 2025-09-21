package cinnamon.gui;

import cinnamon.render.Font;
import cinnamon.utils.IOUtils;
import cinnamon.utils.Resource;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import static cinnamon.events.Events.LOGGER;

public class GUIStyle {

    public static final Resource DEFAULT_STYLE = new Resource("data/gui_styles/default.json");
    private static final Map<Resource, GUIStyle> STYLES_CACHE = new HashMap<>();

    private final Map<String, Object> properties = new HashMap<>();

    private GUIStyle parent;
    private Font font;

    public Object get(String key) {
        if (properties.containsKey(key))
            return properties.get(key);
        if (parent != null)
            return parent.get(key);
        return null;
    }

    public boolean getBoolean(String key) {
        return (boolean) get(key);
    }

    public int getInt(String key) {
        return ((Number) get(key)).intValue();
    }

    public float getFloat(String key) {
        return ((Number) get(key)).floatValue();
    }

    public String getString(String key) {
        return (String) get(key);
    }

    public Resource getResource(String key) {
        return (Resource) get(key);
    }

    public Font getFont() {
        if (font != null)
            return font;
        if (parent != null)
            return parent.getFont();
        return null;
    }

    // -- constructor -- //


    private GUIStyle() {}


    public static void free() {
        STYLES_CACHE.clear();
    }

    public static GUIStyle getDefault() {
        return of(DEFAULT_STYLE);
    }

    public static GUIStyle of(Resource res) {
        GUIStyle style = STYLES_CACHE.get(res);
        if (style != null)
            return style;

        style = createStyle(res);
        STYLES_CACHE.put(res, style);
        return style;
    }

    private static GUIStyle createStyle(Resource res) {
        LOGGER.debug("Loading gui style \"%s\"", res);
        GUIStyle style = new GUIStyle();

        InputStream stream = IOUtils.getResource(res);
        if (stream == null) {
            LOGGER.error("Resource not found: %s", res);
            return style;
        }

        try (stream; InputStreamReader reader = new InputStreamReader(stream)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();

            //parent
            if (json.has("parent"))
                style.parent = of(new Resource(json.get("parent").getAsString()));

            //font
            if (json.has("font")) {
                JsonObject fontJson = json.getAsJsonObject("font");
                Resource path = new Resource(fontJson.get("path").getAsString());
                style.font = Font.getFont(path, fontJson.get("size").getAsInt(),
                        fontJson.has("line_spacing") ? fontJson.get("line_spacing").getAsInt() : 0,
                        fontJson.has("smooth") && fontJson.get("smooth").getAsBoolean());
            }

            //resources
            if (json.has("resources")) {
                JsonObject resources = json.getAsJsonObject("resources");
                for (Map.Entry<String, JsonElement> entry : resources.entrySet())
                    style.properties.put(entry.getKey(), new Resource(entry.getValue().getAsString()));
            }

            //colors
            if (json.has("colors")) {
                JsonObject colors = json.getAsJsonObject("colors");
                for (Map.Entry<String, JsonElement> entry : colors.entrySet())
                    style.properties.put(entry.getKey(), Integer.parseUnsignedInt(entry.getValue().getAsString(), 16));
            }

            //raw values
            if (json.has("primitives")) {
                JsonObject values = json.getAsJsonObject("primitives");
                for (Map.Entry<String, JsonElement> entry : values.entrySet()) {
                    if (entry.getValue().isJsonPrimitive()) {
                        if (entry.getValue().getAsJsonPrimitive().isNumber())
                            style.properties.put(entry.getKey(), entry.getValue().getAsNumber());
                        else if (entry.getValue().getAsJsonPrimitive().isBoolean())
                            style.properties.put(entry.getKey(), entry.getValue().getAsBoolean());
                        else
                            style.properties.put(entry.getKey(), entry.getValue().getAsString());
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load gui style \"%s\"", res, e);
        }

        return style;
    }
}
