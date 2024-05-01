package mayo.registry;

import mayo.model.MaterialManager;
import mayo.model.obj.material.Material;
import mayo.utils.Resource;

public enum MaterialRegistry {

    ACOUSTIC_FOAM,
    ASPHALT_SHINGLE,
    BACTERIA,
    BAMBOO_WOOD,
    BLUE_ICE,
    BRICK_WALL,
    CARBON_FIBER,
    CARPET,
    CHROME,
    CLIFF_ROCK,
    COBBLESTONE,
    CRISSCROSS_FOAM,
    CRYSTAL,
    DISCO_BALL,
    FUR,
    GLASS_FROSTED,
    GOLD,
    GORE,
    GRASS,
    GRASS_MEADOW,
    INFLATABLE_FABRIC,
    LAVA,
    LAVA_ROCK,
    LEATHER_PLAIN,
    METAL_STUDS,
    MILKSHAKE_FOAM,
    ORBED_PLASTIC,
    PADDED_LEATHER,
    PAPER_LANTERN,
    PLASTIC,
    PLASTIC_BRICKS,
    PLYWOOD,
    POOL_TILES,
    QUILTED_FABRIC,
    RIBBED_CHIPPED_METAL,
    RIDGED_FOAM,
    RUBBER,
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
    WHITE_MARBLE;

    public final Resource resource;
    public Material material;

    MaterialRegistry() {
        String name = name().toLowerCase();
        this.resource = new Resource("materials/" + name + "/" + name + ".pbr");
    }

    private void loadMaterial() {
        this.material = MaterialManager.load(this.resource, name().toLowerCase());
    }

    public static void loadAllMaterials() {
        for (MaterialRegistry material : values())
            material.loadMaterial();
    }
}
