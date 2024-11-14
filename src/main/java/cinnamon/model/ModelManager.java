package cinnamon.model;

import cinnamon.parsers.AnimationLoader;
import cinnamon.parsers.ObjLoader;
import cinnamon.render.model.AnimatedObjRenderer;
import cinnamon.render.model.ModelRenderer;
import cinnamon.render.model.ObjRenderer;
import cinnamon.utils.Resource;

import java.util.HashMap;
import java.util.Map;

import static cinnamon.Client.LOGGER;

public class ModelManager {

    private static final Map<Resource, ModelRenderer> MODEL_MAP = new HashMap<>();

    public static ModelRenderer load(Resource resource) {
        //already loaded, return it
        ModelRenderer model = MODEL_MAP.get(resource);
        if (model != null)
            return model;

        LOGGER.info("Loading model {}", resource);

        //load mesh :)
        ModelRenderer newModel;

        //load animations, if any
        String path = resource.getPath();
        String folder = path.substring(0, path.lastIndexOf("/") + 1);
        Resource res = new Resource(resource.getNamespace(), folder + "animations.json");
        try {
            newModel = new AnimatedObjRenderer(ObjLoader.load(resource), AnimationLoader.load(res));
        } catch (Exception ignored) {
            newModel = new ObjRenderer(ObjLoader.load(resource));
        }

        MODEL_MAP.put(resource, newModel);
        return newModel;
    }

    public static void free() {
        for (ModelRenderer value : MODEL_MAP.values())
            value.free();
        MODEL_MAP.clear();
    }
}
