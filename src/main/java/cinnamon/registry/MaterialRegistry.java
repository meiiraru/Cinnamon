package cinnamon.registry;

import cinnamon.model.MaterialManager;
import cinnamon.model.material.Material;
import cinnamon.utils.Resource;

public enum MaterialRegistry {

    DEFAULT(null),
    ACOUSTIC_FOAM,
    ASPHALT_SHINGLE,
    BACTERIA,
    BAMBOO,
    BAMBOO_WOOD,
    BATHROOM_TILES,
    BIRCH_LOG,
    BLUE_ICE,
    BRICK_WALL,
    BROWN_WOOD_PLANKS,
    CARBON_FIBER,
    CARDBOARD,
    CARPET,
    CHROME,
    CLIFF_ROCK,
    COBBLESTONE,
    CORN,
    CRATE,
    CRISSCROSS_FOAM,
    CRYSTAL,
    DISCO_BALL,
    FOIL,
    FUR,
    GLASS_FROSTED,
    GOLD,
    GORE,
    GRANITE,
    GRASS,
    GRASS_MEADOW,
    HEART_PATTERN,
    HONEYCOMB,
    INFLATABLE_FABRIC,
    LAVA,
    LAVA_ROCK,
    LEAF_VEIN,
    LEATHER_PLAIN,
    MAPLE_LEAVES,
    METAL_MESH,
    METAL_STUDS,
    METAL_WEAVE,
    MILKSHAKE_FOAM,
    MOSS,
    NAPKIN,
    OAK_LOG,
    ORBED_PLASTIC,
    PADDED_LEATHER,
    PAPER_LANTERN,
    PAPER_WALL,
    PINE_PLANKS,
    PLAID_FABRIC,
    PLASTER,
    PLASTIC,
    PLASTIC_BRICKS,
    PLYWOOD,
    POOL_TILES,
    QUILTED_FABRIC,
    RAW_SALMON,
    RIBBED_CHIPPED_METAL,
    RIDGED_FOAM,
    RIPPED_CARDBOARD,
    RUBBER,
    RUG,
    RUSTED_IRON,
    SAND,
    SCIFI_PANEL,
    SNOW,
    SPACE_BLANKET,
    STAINLESS_STEEL,
    TERRACOTTA_PAVEMENT,
    TILES_HEXAGONS,
    TILES_MODERN,
    TILES_TRIANGLES,
    VOLCANIC_ROCK,
    WAFFLED_CHIPPED_METAL,
    WATER,
    WHITE_MARBLE,
    WRAPPED_PAPER;

    public final Resource resource;
    public Material material;

    MaterialRegistry() {
        String name = name().toLowerCase();
        this.resource = new Resource("materials/" + name + "/" + name + ".pbr");
    }

    MaterialRegistry(Resource resource) {
        this.resource = resource;
    }

    private void loadMaterial() {
        this.material = resource == null ? null : MaterialManager.load(this.resource, name().toLowerCase());
    }

    public static void loadAllMaterials() {
        for (MaterialRegistry material : values())
            material.loadMaterial();
    }
}
