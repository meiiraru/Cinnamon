package mayo.model;

import mayo.render.Model;
import mayo.utils.Resource;
import org.joml.Vector3f;

public enum LivingEntityModels {
    PICKLE(1.5f),
    STRAWBERRY(0.625f),
    TOMATO(0.75f),
    CHERRY(0.625f),
    ICE_CREAM_SANDWICH(1.375f),
    DONUT(1.875f);

    private static final String MODELS_PATH = "models/entities/";

    public final Resource resource;
    public final float eyeHeight;
    public final Model model;
    public final Vector3f dimensions;

    LivingEntityModels(float eyeHeight) {
        String name = name().toLowerCase();
        this.resource = new Resource(MODELS_PATH + name + "/" + name + ".obj");
        this.eyeHeight = eyeHeight;

        this.model = ModelManager.load(this.resource);
        this.dimensions = model.getMesh().getBoundingBox();
    }

    public static LivingEntityModels random() {
        LivingEntityModels[] models = values();
        return models[(int) (Math.random() * models.length)];
    }
}
