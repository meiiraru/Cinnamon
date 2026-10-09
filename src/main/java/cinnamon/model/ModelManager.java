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

/**
 * Manages loading and caching of {@link ModelRenderer} and {@link Mesh} instances
 */
public final class ModelManager {

    private ModelManager() {}

    private static final Map<Resource, ModelRenderer> RENDERERS = new HashMap<>();
    private static final Map<Resource, Mesh> MESHES = new HashMap<>();

    /**
     * Gets a {@link ModelRenderer} for the given resource, loading and caching it if necessary
     * @param resource The {@link Resource} of the model to get
     * @return The {@link ModelRenderer} for the given resource, or {@code null} if it could not be loaded
     */
    public static ModelRenderer getRenderer(Resource resource) {
        if (resource == null)
            return null;

        ModelRenderer renderer = getCachedRenderer(resource);
        if (renderer != null)
            return renderer instanceof AnimatedMeshRenderer anim ? new AnimatedMeshRenderer(anim) : renderer;

        //bake and cache
        ModelRenderer newRenderer = bakeModel(resource);
        return cacheRenderer(resource, newRenderer);
    }

    /**
     * Gets a {@link Mesh} for the given resource, loading and caching it if necessary
     * @param resource The {@link Resource} of the model to get
     * @return The {@link Mesh} for the given resource, or {@code null} if it could not be loaded
     * @see #getMesh(Resource, boolean)
     */
    public static Mesh getMesh(Resource resource) {
        return getMesh(resource, true);
    }

    /**
     * Gets a {@link Mesh} for the given resource, loading and caching it if necessary<br>
     * The mesh may also be optimized during loading, which can reduce the number of vertices and improve performance
     * @param resource The {@link Resource} of the model to get
     * @param optimizeMesh Whether to optimize the mesh during loading
     * @return The {@link Mesh} for the given resource, or {@code null} if it could not be loaded
     */
    public static Mesh getMesh(Resource resource, boolean optimizeMesh) {
        if (resource == null)
            return null;

        //find from cache
        Mesh mesh = getCachedMesh(resource);
        if (mesh != null)
            return mesh;

        //otherwise load and cache
        Mesh newMesh = loadMesh(resource);

        //optimize loaded mesh
        if (optimizeMesh && newMesh != null) {
            MeshHelper.clearUnusedVertices(newMesh);
            MeshHelper.stripDuplicateVertices(newMesh);
        }

        return cacheMesh(resource, newMesh);
    }

    /**
     * Checks if a {@link ModelRenderer} has been loaded and cached for the given resource
     * @param resource The {@link Resource} of the model to check
     * @return {@code true} if the model has a loaded and cached renderer, {@code false} otherwise
     */
    public static boolean hasRenderer(Resource resource) {
        return getCachedRenderer(resource) != null;
    }

    /**
     * Checks if a {@link Mesh} has been loaded and cached for the given resource
     * @param resource The {@link Resource} of the model to check
     * @return {@code true} if the model has been loaded and cached, {@code false} otherwise
     */
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

    /**
     * Frees all cached {@link ModelRenderer} and {@link Mesh}
     */
    public static void free() {
        for (ModelRenderer value : RENDERERS.values())
            value.free();
        RENDERERS.clear();
        MESHES.clear();
    }

    /**
     * Frees a specific {@link ModelRenderer} and {@link Mesh} from the cache
     * @param resource The {@link Resource} of the model to free
     */
    public static void free(Resource resource) {
        ModelRenderer renderer = RENDERERS.remove(resource);
        if (renderer != null)
            renderer.free();
        MESHES.remove(resource);
    }
}
