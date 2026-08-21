package cinnamon.model;

import cinnamon.model.material.Material;
import cinnamon.parsers.MaterialLoader;
import cinnamon.utils.Resource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cinnamon.events.Events.LOGGER;

public class MaterialManager {

    private static final Map<Resource, Map<String, Material>> MATERIAL_MAP = new HashMap<>();

    //return the cached material or load and cache a new one
    private static Map<String, Material> getOrLoad(Resource resource) {
        //try cache
        Map<String, Material> materialMap = MATERIAL_MAP.get(resource);
        if (materialMap != null)
            return materialMap;

        //try loading
        try {
            materialMap = MaterialLoader.load(resource);
        } catch (Exception e) {
            LOGGER.error("Failed to load material \"%s\"", resource, e);
            materialMap = new HashMap<>();
        }

        //cache and return
        MATERIAL_MAP.put(resource, materialMap);
        return materialMap;
    }

    public static List<Material> get(Resource resource) {
        return List.copyOf(getOrLoad(resource).values());
    }

    public static Material get(Resource resource, String id) {
        return getOrLoad(resource).get(id);
    }

    public static void free() {
        MATERIAL_MAP.clear();
    }
}
