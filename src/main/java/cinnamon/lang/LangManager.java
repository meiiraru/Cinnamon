package cinnamon.lang;

import cinnamon.settings.Settings;
import cinnamon.utils.IOUtils;
import cinnamon.utils.Resource;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cinnamon.Client.LOGGER;

public class LangManager {

    public static final String MAIN_LANG = "en_UK";

    private static final Map<String, String> LANG = new HashMap<>();

    public static void init() {
        LANG.clear();

        //load the namespaces
        List<String> namespaces = IOUtils.listNamespaces();
        namespaces.sort((a, b) -> {
            if (a.equals(Resource.VANILLA_NAMESPACE))
                return -1;
            if (b.equals(Resource.VANILLA_NAMESPACE))
                return 1;
            return 0;
        });

        //get the current lang
        String currLang = Settings.lang.get();
        LOGGER.info("Initializing lang for: " + currLang);

        //load the namespaces lang
        for (String s : namespaces) {
            //start with the main lang
            load(new Resource(s, "lang/" + MAIN_LANG + ".json"));

            //then load the current lang
            if (!currLang.equals(MAIN_LANG))
                load(new Resource(s, "lang/" + currLang + ".json"));
        }
    }

    private static void load(Resource res) {
        if (!IOUtils.hasResource(res))
            return;

        LOGGER.debug("Loading lang file: " + res);

        try {
            JsonObject json = JsonParser.parseReader(new InputStreamReader(IOUtils.getResource(res))).getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue().getAsString();
                LANG.put(key, value);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load lang file: " + res, e);
        }
    }

    public static String get(String key) {
        return LANG.getOrDefault(key, key);
    }
}
