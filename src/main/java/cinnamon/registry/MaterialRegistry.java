package cinnamon.registry;

import cinnamon.model.MaterialManager;
import cinnamon.model.material.Material;
import cinnamon.utils.Resource;

public enum MaterialRegistry {

    DEFAULT(null),
    BLACK_TILES,
    BRICK_WALL,
    BROWN_WOOD_PLANKS,
    CHECKERED_TILES,
    CHROME,
    COBBLESTONE,
    CRATE,
    FOIL,
    GLASS_FROSTED,
    GOLD,
    GRASS,
    LATEX,
    LEATHER,
    LEATHER_PADDED,
    OBSIDIAN,
    PINE_PLANKS,
    TERRAZZO_SLAB,
    WHITE_CONCRETE,
    YELLOW_CONCRETE,
    DEBUG,
    DEBUG_UV;

    public static final Material MISSING = new Material("missing");

    public final Resource resource;

    MaterialRegistry() {
        String name = name().toLowerCase();
        this.resource = new Resource("materials/common/" + name + "/" + name + ".mtl");
    }

    MaterialRegistry(Resource resource) {
        this.resource = resource;
    }

    public Material getMaterial() {
        if (resource == null)
            return null;

        for (Material value : MaterialManager.get(this.resource).values())
            return value;

        return null;
    }

    public static MaterialRegistry findByMaterial(Material material) {
        if (material == null)
            return DEFAULT;

        for (MaterialRegistry m : values()) {
            if (m.getMaterial() == material)
                return m;
        }
        return null;
    }
}
