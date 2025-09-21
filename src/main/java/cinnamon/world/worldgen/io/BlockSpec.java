package cinnamon.world.worldgen.io;

import cinnamon.registry.MaterialRegistry;
import cinnamon.registry.TerrainRegistry;

/**
 * Serializable block descriptor used in chunk palettes.
 */
public class BlockSpec {
    public TerrainRegistry terrain; // e.g., BOX
    public MaterialRegistry material; // e.g., GRASS
    public byte rot; // rotation 0..3 (Y axis)

    public BlockSpec() {}

    public BlockSpec(TerrainRegistry terrain, MaterialRegistry material, byte rot) {
        this.terrain = terrain;
        this.material = material;
        this.rot = rot;
    }
}
