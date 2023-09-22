package mayo.model;

import mayo.model.obj.Mesh;
import mayo.utils.Resource;
import org.joml.Vector3f;

public enum LivingEntityModels {
    PICKLE(1.5f),
    STRAWBERRY(0.625f),
    TOMATO(0.75f);

    private static final String MODELS_PATH = "models/entities/";

    public final Resource resource;
    public final float eyeHeight;
    public final Mesh mesh;
    public final Vector3f dimensions;

    LivingEntityModels(float eyeHeight) {
        String name = name().toLowerCase();
        this.resource = new Resource(MODELS_PATH + name + "/" + name + ".obj");
        this.eyeHeight = eyeHeight;

        this.mesh = ModelManager.load(this.resource);
        this.dimensions = mesh.getBoundingBox();
    }

    public static LivingEntityModels random() {
        LivingEntityModels[] models = values();
        return models[(int) (Math.random() * models.length)];
    }
}
