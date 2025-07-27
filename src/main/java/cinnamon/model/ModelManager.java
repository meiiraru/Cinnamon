package cinnamon.model;

import cinnamon.model.obj.Mesh;
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

        ModelRenderer model = getCache(resource);
        if (model != null)
            return model instanceof AnimatedObjRenderer anim ? new AnimatedObjRenderer(anim) : model;

        //bake and cache
        return cacheModel(resource, bakeModel(resource));
    }

    private static ModelRenderer getCache(Resource resource) {
        return resource == null ? null : MODEL_MAP.get(resource);
    }

    private static ModelRenderer cacheModel(Resource resource, ModelRenderer model) {
        if (model != null)
            MODEL_MAP.put(resource, model);
        return model;
    }

    private static ModelRenderer bakeModel(Resource resource) {
        ModelRenderer model;

        try {
            //check model type
            if (resource.getExtension().equalsIgnoreCase("obj")) { //prefer built-in OBJ loader
                Mesh mesh = ObjLoader.load(resource);
                //load animations, if any
                Resource anim = resource.resolveSibling("animations.json");
                if (IOUtils.hasResource(anim)) {
                    try {
                        model = new AnimatedObjRenderer(mesh, AnimationLoader.load(anim));
                    } catch (Exception e) {
                        LOGGER.error("Failed to load animations for model \"%s\"", resource, e);
                        model = new ObjRenderer(mesh);
                    }
                } else {
                    model = new ObjRenderer(mesh);
                }
            } else { //otherwise use Assimp
                model = new AssimpRenderer(AssimpLoader.load(resource));
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load model \"%s\"", resource, e);
            model = null;
        }

        return model;
    }

    public static void free() {
        for (ModelRenderer value : MODEL_MAP.values())
            value.free();
        MODEL_MAP.clear();
    }
}
