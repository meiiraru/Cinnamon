package cinnamon.lang;

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

public class LangManager {

    public static final String MAIN_LANG = "en_UK";
    private static String currentLang = MAIN_LANG;
    private static boolean loaded = false;

    private static final Map<String, String>
            LANG = new HashMap<>(),
            LANG_LIST = new HashMap<>();

    public static void init() {
        //load lang list
        loadLangList();
        //force load the current lang
        if (!loaded)
            loadForLang(currentLang);
    }

    public static void loadForLang(String lang) {
        loaded = false;
        currentLang = lang == null ? MAIN_LANG : lang;

        LANG.clear();

        //get the current lang
        LOGGER.info("Initializing lang for: %s", currentLang);

        //load the namespaces lang
        for (String s : IOUtils.listNamespacesVanillaFirst()) {
            //start with the main lang
            load(new Resource(s, "lang/" + MAIN_LANG + ".json"));

            //then load the current lang
            if (!currentLang.equals(MAIN_LANG))
                load(new Resource(s, "lang/" + currentLang + ".json"));
        }

        loaded = true;
    }

    private static void load(Resource res) {
        if (!IOUtils.hasResource(res))
            return;

        LOGGER.debug("Loading lang \"%s\"", res);

        try (InputStream stream = IOUtils.getResource(res); InputStreamReader reader = new InputStreamReader(stream)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue().getAsString();
                LANG.put(key, value);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load lang \"%s\"", res, e);
        }
    }

    public static String get(String key, Object... args) {
        String value = LANG.getOrDefault(key, key);
        return args == null || args.length == 0 ? value : String.format(value, args);
    }

    private static void loadLangList() {
        LANG_LIST.clear();

        //load the lang list
        for (String s : IOUtils.listNamespacesVanillaFirst()) {
            Resource res = new Resource(s, "lang/langs.json");
            if (!IOUtils.hasResource(res))
                continue;

            try (InputStream stream = IOUtils.getResource(res); InputStreamReader reader = new InputStreamReader(stream)) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                for (Map.Entry<String, JsonElement> entry : json.asMap().entrySet())
                    LANG_LIST.put(entry.getKey(), entry.getValue().getAsString());
            } catch (Exception e) {
                LOGGER.error("Failed to load lang data \"%s\"", res, e);
            }
        }
    }

    public static Map<String, String> getLangList() {
        return LANG_LIST;
    }

    public static String getCurrentLang() {
        return currentLang;
    }
}
