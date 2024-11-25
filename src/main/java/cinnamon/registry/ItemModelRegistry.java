package cinnamon.registry;

import cinnamon.utils.Resource;

public enum ItemModelRegistry {

    //misc
    MAGIC_WAND("Magic Wand", "models/items/magic_wand/magic_wand.obj"),
    FLASHLIGHT("Flashlight", "models/items/flashlight/flashlight.obj"),
    CURVE_MAKER("Curve Maker", "models/items/curve_maker/curve_maker.obj"),
    BUBBLE_GUN("Bubble Gun", "models/items/bubble_gun/bubble_gun.obj"),

    //weapons
    COIL_GUN("Coil Gun", "models/items/coil_gun/coil_gun.obj"),
    POTATO_CANNON("Potato Cannon", "models/items/potato_cannon/potato_cannon.obj"),
    RICE_GUN("Rice Gun", "models/items/rice_gun/rice_gun.obj");

    public final String id;
    public final Resource resource;

    ItemModelRegistry(String id, String path) {
        this.id = id;
        this.resource = new Resource(path);
    }
}
