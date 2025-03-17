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

    private static final Map<String, String>
            LANG = new HashMap<>(),
            LANG_LIST = new HashMap<>();

    public static void init() {
        //load current lang
        loadForLang(Settings.lang.get());
        //load lang list
        loadLangList();
    }

    public static void loadForLang(String lang) {
        LANG.clear();

        //get the current lang
        LOGGER.info("Initializing lang for: " + lang);

        //load the namespaces lang
        for (String s : getNamespaces()) {
            //start with the main lang
            load(new Resource(s, "lang/" + MAIN_LANG + ".json"));

            //then load the current lang
            if (!lang.equals(MAIN_LANG))
                load(new Resource(s, "lang/" + lang + ".json"));
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

    public static String get(String key, Object... args) {
        String value = LANG.getOrDefault(key, key);
        return args == null || args.length == 0 ? value : String.format(value, args);
    }

    private static void loadLangList() {
        LANG_LIST.clear();

        //load the lang list
        for (String s : getNamespaces()) {
            Resource res = new Resource(s, "lang/langs.json");
            if (!IOUtils.hasResource(res))
                continue;

            try {
                JsonObject json = JsonParser.parseReader(new InputStreamReader(IOUtils.getResource(res))).getAsJsonObject();
                for (Map.Entry<String, JsonElement> entry : json.asMap().entrySet())
                    LANG_LIST.put(entry.getKey(), entry.getValue().getAsString());
            } catch (Exception e) {
                LOGGER.error("Failed to load lang data: " + res, e);
            }
        }
    }

    public static Map<String, String> getLangList() {
        return LANG_LIST;
    }

    private static List<String> getNamespaces() {
        List<String> namespaces = IOUtils.listNamespaces();
        namespaces.sort((a, b) -> {
            if (a.equals(Resource.VANILLA_NAMESPACE))
                return -1;
            if (b.equals(Resource.VANILLA_NAMESPACE))
                return 1;
            return 0;
        });
        return namespaces;
    }
}
