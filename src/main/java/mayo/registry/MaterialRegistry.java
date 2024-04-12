package mayo.registry;

import mayo.model.MaterialManager;
import mayo.model.obj.material.Material;
import mayo.utils.Resource;

public enum MaterialRegistry {

    ACOUSTIC_FOAM,
    BAMBOO_WOOD,
    BRICK_WALL,
    GORE,
    GRASS_MEADOW,
    LEATHER_PLAIN,
    RIDGED_FOAM,
    RUSTED_IRON,
    SCIFI_PANEL,
    SPACE_BLANKET,
    STAINLESS_STEEL,
    TERRACOTTA_PAVEMENT,
    VOLCANIC_ROCK,
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
