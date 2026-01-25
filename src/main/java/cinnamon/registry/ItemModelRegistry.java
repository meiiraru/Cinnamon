package cinnamon.registry;

import cinnamon.utils.Resource;

public enum ItemModelRegistry {

    //misc
    MAGIC_WAND("item.magic_wand", "models/items/magic_wand/magic_wand.obj"),
    FLASHLIGHT("item.flashlight", "models/items/flashlight/flashlight.obj"),
    CURVE_MAKER("item.curve_maker", "models/items/curve_maker/curve_maker.obj"),
    BUBBLE_GUN("item.bubble_gun", "models/items/bubble_gun/bubble_gun.obj"),
    POTATO("item.potato", "models/entities/projectile/potato/potato.obj"),
    PAINT_GUN("item.paint_gun", "models/items/paint_gun/paint_gun.obj"),

    //weapons
    COIL_GUN("item.coil_gun", "models/items/coil_gun/coil_gun.obj"),
    POTATO_CANNON("item.potato_cannon", "models/items/potato_cannon/potato_cannon.obj"),
    RICE_GUN("item.rice_gun", "models/items/rice_gun/rice_gun.obj");

    public final String id;
    public final Resource resource;

    ItemModelRegistry(String id, String path) {
        this.id = id;
        this.resource = new Resource(path);
    }
}
