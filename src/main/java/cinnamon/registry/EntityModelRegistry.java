package cinnamon.registry;

import cinnamon.model.ModelManager;
import cinnamon.render.model.ModelRenderer;
import cinnamon.utils.Resource;

public enum EntityModelRegistry {

    //collectables
    EFFECT_BOX("models/entities/collectable/milkshake/milkshake.obj"),
    HEALTH_PACK("models/entities/collectable/ramen/ramen.obj"),

    //projectiles
    CANDY("models/entities/projectile/candy/candy.obj"),
    POTATO("models/entities/projectile/potato/potato.obj"),
    RICE("models/entities/projectile/rice/rice.obj"),
    RICE_BALL("models/entities/projectile/rice/rice.obj"),

    //vehicles
    CART("models/entities/vehicle/cart/cart.obj");

    public final Resource resource;
    public ModelRenderer model;

    EntityModelRegistry(String path) {
        this.resource = new Resource(path);
    }

    private void loadModel() {
        this.model = ModelManager.load(resource);
    }

    public static void loadAllModels() {
        for (EntityModelRegistry entityModelRegistry : values())
            entityModelRegistry.loadModel();
    }
}
