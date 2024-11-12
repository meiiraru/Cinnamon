package cinnamon.model;

import cinnamon.parsers.AnimationLoader;
import cinnamon.parsers.ObjLoader;
import cinnamon.render.AnimatedModel;
import cinnamon.render.MeshModel;
import cinnamon.render.Model;
import cinnamon.utils.Resource;

import java.util.HashMap;
import java.util.Map;

import static cinnamon.Client.LOGGER;

public class ModelManager {

    private static final Map<Resource, Model> MODEL_MAP = new HashMap<>();

    public static Model load(Resource resource) {
        //already loaded, return it
        Model model = MODEL_MAP.get(resource);
        if (model != null)
            return model;

        LOGGER.info("Loading model {}", resource);

        //load mesh :)
        Model newModel;

        //load animations, if any
        String path = resource.getPath();
        String folder = path.substring(0, path.lastIndexOf("/") + 1);
        Resource res = new Resource(resource.getNamespace(), folder + "animations.json");
        try {
            newModel = new AnimatedModel(ObjLoader.load(resource), AnimationLoader.load(res));
        } catch (Exception ignored) {
            newModel = new MeshModel(ObjLoader.load(resource));
        }

        MODEL_MAP.put(resource, newModel);
        return newModel;
    }

    public static void free() {
        for (Model value : MODEL_MAP.values())
            value.free();
        MODEL_MAP.clear();
    }
}
