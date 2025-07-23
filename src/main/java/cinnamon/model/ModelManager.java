package cinnamon.model;

import cinnamon.parsers.AnimationLoader;
import cinnamon.parsers.AssimpLoader;
import cinnamon.parsers.ObjLoader;
import cinnamon.render.model.AnimatedObjRenderer;
import cinnamon.render.model.AssimpRenderer;
import cinnamon.render.model.ModelRenderer;
import cinnamon.render.model.ObjRenderer;
import cinnamon.utils.IOUtils;
import cinnamon.utils.Resource;

import java.util.HashMap;
import java.util.Map;

import static cinnamon.events.Events.LOGGER;

public class ModelManager {

    private static final Map<Resource, ModelRenderer> MODEL_MAP = new HashMap<>();

    public static ModelRenderer load(Resource resource) {
        if (resource == null)
            return null;

        //already loaded, return it
        ModelRenderer model = MODEL_MAP.get(resource);
        if (model != null)
            return model instanceof AnimatedObjRenderer anim ? new AnimatedObjRenderer(anim) : model;

        //load new and cache
        model = bakeModel(resource);
        MODEL_MAP.put(resource, model);
        return model;
    }

    private static ModelRenderer bakeModel(Resource resource) {
        ModelRenderer model;

        //check model type
        String path = resource.getPath();
        if (path.endsWith(".obj")) { //prefer built-in OBJ loader
            //load animations, if any
            String folder = path.substring(0, path.lastIndexOf("/") + 1);
            Resource anim = new Resource(resource.getNamespace(), folder + "animations.json");
            if (IOUtils.hasResource(anim)) {
                model = new AnimatedObjRenderer(ObjLoader.load(resource), AnimationLoader.load(anim));
            } else {
                model = new ObjRenderer(ObjLoader.load(resource));
            }
        } else { //otherwise use Assimp
            model = new AssimpRenderer(AssimpLoader.load(resource));
        }

        return model;
    }

    public static void free() {
        for (ModelRenderer value : MODEL_MAP.values())
            value.free();
        MODEL_MAP.clear();
    }
}
