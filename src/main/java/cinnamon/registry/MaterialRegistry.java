package cinnamon.registry;

import cinnamon.model.MaterialManager;
import cinnamon.model.material.Material;
import cinnamon.utils.Resource;

public enum MaterialRegistry {

    DEFAULT(null),
    ACOUSTIC_FOAM,
    ALIEN_CARNIVEROUS_PLANT,
    ALIEN_SLIME,
    ASPHALT_SHINGLE,
    BACTERIA,
    BAMBOO,
    BAMBOO_WOOD,
    BASE_WHITE_TILES,
    BATHROOM_TILES,
    BIRCH_LOG,
    BLACK_TILES,
    BLUE_ICE,
    BRICK_WALL,
    BROKEN_DOWN_CONCRETE,
    BROWN_WOOD_PLANKS,
    CANDY,
    CARBON_FIBER,
    CARDBOARD,
    CARPET,
    CARPET2,
    CERAMIC_TILES,
    CHECKERED_TILES,
    CHISELED_COBBLE,
    CHROME,
    CLAY_SHINGLES,
    CLIFF_ROCK,
    COBBLESTONE,
    COBBLESTONE2,
    CORN,
    CRATE,
    CRISSCROSS_FOAM,
    CRYSTAL,
    DIRT,
    DISCO_BALL,
    FABRIC,
    FACADE,
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
    LATEX,
    LAVA,
    LAVA_ROCK,
    LEAF_VEIN,
    LEATHER,
    LEATHER2,
    LEATHER_PLAIN,
    LEATHER_SKIN,
    MAPLE_LEAVES,
    METAL_FENCE,
    METAL_MESH,
    METAL_STUDS,
    METAL_VENTILATION,
    METAL_WEAVE,
    MILKSHAKE_FOAM,
    MOSS,
    NAPKIN,
    OAK_LOG,
    OBSIDIAN,
    OFFICE_CEILING,
    ONYX,
    ORBED_PLASTIC,
    ORNATE_CELTIC_GOLD,
    PADDED_LEATHER,
    PADDED_LEATHER2,
    PAPER_LANTERN,
    PAPER_WALL,
    PINE_PLANKS,
    PLAID_FABRIC,
    PLASTER,
    PLASTIC,
    PLASTIC_BRICKS,
    PLYWOOD,
    POOL_TILES,
    POOL_TILES2,
    QUILTED_FABRIC,
    QUILTED_FABRIC2,
    RATTAN_WEAVE,
    RAW_SALMON,
    RIBBED_CHIPPED_METAL,
    RIDGED_FOAM,
    RIPPED_CARDBOARD,
    ROCKY_SHORELINE,
    RUBBER,
    RUG,
    RUSTED_IRON,
    SAND,
    SCIFI_PANEL,
    SHEET_METAL,
    SNOW,
    SOLAR_PANEL,
    SPACE_BLANKET,
    STACKED_STONE,
    STAINLESS_STEEL,
    TERRACOTTA_PAVEMENT,
    TERRAZZO_SLAB,
    TILES_HEXAGONS,
    TILES_MODERN,
    TILES_TRIANGLES,
    VOLCANIC_ROCK,
    WAFFLED_CHIPPED_METAL,
    WATER,
    WHITE_CONCRETE,
    WHITE_MARBLE,
    WRAPPED_PAPER,
    YELLOW_CONCRETE,
    DEBUG,
    DEBUG_UV;

    public static final Material MISSING = new Material("missing");

    public final Resource resource;
    public Material material;

    MaterialRegistry() {
        String name = name().toLowerCase();
        this.resource = new Resource("materials/" + name + "/" + name + ".mtl");
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

    public static MaterialRegistry findByMaterial(Material material) {
        if (material == null)
            return DEFAULT;

        for (MaterialRegistry m : values()) {
            if (m.material == material)
                return m;
        }
        return null;
    }
}
