package mayo.registry;

import mayo.model.ModelManager;
import mayo.render.Model;
import mayo.utils.Resource;

public enum LivingModelRegistry {
    PICKLE(1.5f),
    STRAWBERRY(0.625f),
    TOMATO(0.75f),
    CHERRY(0.625f),
    ICE_CREAM_SANDWICH(1.375f),
    DONUT(1.875f),
    ICE_CREAM(0.75f),
    PANCAKE(1.125f),
    COXINHA(0.75f),
    DUMMY(2f);

    private static final String MODELS_PATH = "models/entities/living/";

    public final Resource resource;
    public final float eyeHeight;
    public Model model;

    LivingModelRegistry(float eyeHeight) {
        String name = name().toLowerCase();
        this.resource = new Resource(MODELS_PATH + name + "/" + name + ".obj");
        this.eyeHeight = eyeHeight;
    }

    private void loadModel() {
        this.model = ModelManager.load(this.resource);
    }

    public static LivingModelRegistry random() {
        LivingModelRegistry[] models = values();
        return models[(int) (Math.random() * models.length)];
    }

    public static void loadAllModels() {
        for (LivingModelRegistry livingModelRegistry : values())
            livingModelRegistry.loadModel();
    }
}
