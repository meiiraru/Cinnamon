package mayo.registry;

import mayo.model.MaterialManager;
import mayo.model.obj.material.Material;
import mayo.utils.Resource;

public enum MaterialRegistry {

    ACOUSTIC_FOAM,
    ASPHALT_SHINGLE,
    BAMBOO_WOOD,
    BRICK_WALL,
    CARBON_FIBER,
    CARPET,
    COBBLESTONE,
    CRISSCROSS_FOAM,
    FUR,
    GORE,
    GRASS_MEADOW,
    LAVA_ROCK,
    LEATHER_PLAIN,
    METAL_STUDS,
    ORBED_PLASTIC,
    PADDED_LEATHER,
    PLASTIC,
    QUILTED_DIAMOND,
    RIBBED_CHIPPED_METAL,
    RIDGED_FOAM,
    RUSTED_IRON,
    SCIFI_PANEL,
    SPACE_BLANKET,
    STAINLESS_STEEL,
    TERRACOTTA_PAVEMENT,
    VOLCANIC_ROCK,
    WAFFLED_CHIPPED_METAL,
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
