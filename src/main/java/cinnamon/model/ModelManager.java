package cinnamon.model;

import cinnamon.parsers.AnimationLoader;
import cinnamon.parsers.GLTFLoader;
import cinnamon.parsers.ObjLoader;
import cinnamon.render.model.AnimatedObjRenderer;
import cinnamon.render.model.GLTFRenderer;
import cinnamon.render.model.ModelRenderer;
import cinnamon.render.model.ObjRenderer;
import cinnamon.utils.IOUtils;
import cinnamon.utils.Resource;

import java.util.HashMap;
import java.util.Map;

import static cinnamon.Client.LOGGER;

public class ModelManager {

    private static final Map<Resource, ModelRenderer> MODEL_MAP = new HashMap<>();

    public static ModelRenderer load(Resource resource) {
        if (resource == null)
            return null;

        //already loaded, return it
        ModelRenderer model = MODEL_MAP.get(resource);
        if (model != null)
            return model instanceof AnimatedObjRenderer anim ? new AnimatedObjRenderer(anim) : model;

        LOGGER.debug("Loading model {}", resource);

        //load mesh :)
        ModelRenderer newModel;

        //check model type
        if (resource.getPath().endsWith(".gltf")) {
            newModel = new GLTFRenderer(GLTFLoader.load(resource));
        } else {
            //load animations, if any
            String path = resource.getPath();
            String folder = path.substring(0, path.lastIndexOf("/") + 1);
            Resource anim = new Resource(resource.getNamespace(), folder + "animations.json");
            if (IOUtils.hasResource(anim)) {
                newModel = new AnimatedObjRenderer(ObjLoader.load(resource), AnimationLoader.load(anim));
            } else {
                newModel = new ObjRenderer(ObjLoader.load(resource));
            }
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
