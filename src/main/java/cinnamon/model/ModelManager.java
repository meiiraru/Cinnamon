package cinnamon.model;

import cinnamon.model.mesh.Mesh;
import cinnamon.parsers.AssimpLoader;
import cinnamon.parsers.ObjLoader;
import cinnamon.render.model.AnimatedMeshRenderer;
import cinnamon.render.model.MeshRenderer;
import cinnamon.render.model.ModelRenderer;
import cinnamon.utils.Resource;

import java.util.HashMap;
import java.util.Map;

import static cinnamon.events.Events.LOGGER;

public class ModelManager {

    private static final Map<Resource, ModelRenderer> RENDERERS = new HashMap<>();
    private static final Map<Resource, Mesh> MESHES = new HashMap<>();

    public static ModelRenderer getRenderer(Resource resource) {
        if (resource == null)
            return null;

        ModelRenderer model = getCachedRenderer(resource);
        if (model != null)
            return model instanceof AnimatedMeshRenderer anim ? new AnimatedMeshRenderer(anim) : model;

        //bake and cache
        return cacheRenderer(resource, bakeModel(resource));
    }

    public static Mesh getMesh(Resource resource) {
        if (resource == null)
            return null;

        Mesh mesh = getCachedMesh(resource);
        if (mesh != null)
            return mesh;

        //cache and return
        return cacheMesh(resource, loadMesh(resource));
    }

    public static boolean hasRenderer(Resource resource) {
        return getCachedRenderer(resource) != null;
    }

    public static boolean hasModel(Resource resource) {
        return getCachedMesh(resource) != null;
    }

    private static ModelRenderer getCachedRenderer(Resource resource) {
        return resource == null ? null : RENDERERS.get(resource);
    }

    private static Mesh getCachedMesh(Resource resource) {
        return resource == null ? null : MESHES.get(resource);
    }

    private static ModelRenderer cacheRenderer(Resource resource, ModelRenderer model) {
        if (model != null)
            RENDERERS.put(resource, model);
        return model;
    }

    private static Mesh cacheMesh(Resource resource, Mesh mesh) {
        if (mesh != null)
            MESHES.put(resource, mesh);
        return mesh;
    }

    private static ModelRenderer bakeModel(Resource resource) {
        Mesh mesh = getMesh(resource);
        if (mesh == null)
            return null;

        return mesh.getAnimationData() != null ? new AnimatedMeshRenderer(mesh) : new MeshRenderer(mesh);
    }

    private static Mesh loadMesh(Resource resource) {
        try {
            //check model type
            String extension = resource.getExtension();
            if (extension.equalsIgnoreCase("obj")) {
                return ObjLoader.load(resource);
            //} else if (extension.equalsIgnoreCase("bbmodel")) { //blockbench model
            //    BBModelLoader.BBModelData modelData = BBModelLoader.load(resource);
            //    model = new AnimatedObjRenderer(modelData.mesh(), modelData.rootBone(), modelData.animations());
            } else { //otherwise use Assimp
                return AssimpLoader.load(resource);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load model \"%s\"", resource, e);
            return null;
        }
    }

    public static void free() {
        for (ModelRenderer value : RENDERERS.values())
            value.free();
        RENDERERS.clear();
        MESHES.clear();
    }
}
