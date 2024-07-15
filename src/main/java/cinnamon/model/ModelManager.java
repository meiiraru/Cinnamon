package cinnamon.model;

import cinnamon.parsers.ObjLoader;
import cinnamon.render.Model;
import cinnamon.utils.Resource;

import java.util.HashMap;
import java.util.Map;

public class ModelManager {

    private static final Map<Resource, Model> MODEL_MAP = new HashMap<>();

    public static Model load(Resource resource) {
        //already loaded, return it
        Model model = MODEL_MAP.get(resource);
        if (model != null)
            return model;

        //load mesh :)
        Model newModel = new Model(ObjLoader.load(resource));
        MODEL_MAP.put(resource, newModel);
        return newModel;
    }

    public static void free() {
        for (Model value : MODEL_MAP.values())
            value.free();
        MODEL_MAP.clear();
    }
}
