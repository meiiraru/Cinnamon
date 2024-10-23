package cinnamon.model;

import cinnamon.model.material.Material;
import cinnamon.parsers.MaterialLoader;
import cinnamon.utils.Resource;

import java.util.HashMap;
import java.util.Map;

public class MaterialManager {

    private static final Map<Resource, Map<String, Material>> MATERIAL_MAP = new HashMap<>();

    public static Material load(Resource resource, String id) {
        //check if the material is already loaded
        //if not, load the material and add it to the map
        //then return the material even if it is null
        Map<String, Material> materialMap = MATERIAL_MAP.get(resource);
        if (materialMap == null) {
            materialMap = MaterialLoader.load(resource);
            MATERIAL_MAP.put(resource, materialMap);
        }
        return materialMap.get(id);
    }

    public static void free() {
        MATERIAL_MAP.clear();
    }
}
