package cinnamon.registry;

import cinnamon.utils.Resource;

public enum MiscModelRegistry {

    //unused stuff
    COIN("models/entities/collectable/coin/coin.obj"),
    MYSTERY_BOX("models/entities/collectable/mystery_box/mystery_box.obj"),
    HEART("models/entities/collectable/heart/crystal_heart.obj"),
    BUBBLE_TOY("models/items/bubble_toy/bubble_toy.obj"),

    //misc
    CROSS("models/misc/cross.obj"),
    DIAMOND("models/misc/diamond.obj"),
    INVERTED_SPHERE("models/misc/inverted_sphere.obj"),

    //xr
    XR_HAND("models/xr/hands/paw.obj");

    public final Resource resource;

    MiscModelRegistry(String path) {
        this.resource = new Resource(path);
    }
}
