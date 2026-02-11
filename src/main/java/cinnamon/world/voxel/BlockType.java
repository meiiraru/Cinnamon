package cinnamon.world.voxel;

import cinnamon.registry.MaterialRegistry;

/**
 * Block types for the voxel world.
 * Each block is stored as a short ID in chunk arrays — no per-block Java objects.
 * ID 0 is always AIR (empty space).
 */
public enum BlockType {
    AIR(null, false, false),
    GRASS(MaterialRegistry.GRASS, true, false),
    DIRT(MaterialRegistry.COBBLESTONE, true, false), // using cobblestone as dirt-like
    STONE(MaterialRegistry.GRANITE, true, false),
    SAND(MaterialRegistry.SAND, true, false),
    GOLD(MaterialRegistry.GOLD, true, false),
    SNOW(MaterialRegistry.SNOW, true, false),
    WATER(MaterialRegistry.WATER, false, true),
    LAVA(MaterialRegistry.LAVA, true, false),
    BRICK(MaterialRegistry.BRICK_WALL, true, false),
    WOOD(MaterialRegistry.PINE_PLANKS, true, false),
    LEAVES(MaterialRegistry.MAPLE_LEAVES, true, false),
    LOG(MaterialRegistry.OAK_LOG, true, false),
    MOSS(MaterialRegistry.MOSS, true, false),
    IRON(MaterialRegistry.RUSTED_IRON, true, false),
    MARBLE(MaterialRegistry.WHITE_MARBLE, true, false);

    public static final BlockType[] VALUES = values();
    public static final int COUNT = VALUES.length;

    public final MaterialRegistry material;
    public final boolean solid;
    public final boolean transparent;

    BlockType(MaterialRegistry material, boolean solid, boolean transparent) {
        this.material = material;
        this.solid = solid;
        this.transparent = transparent;
    }

    public boolean isAir() {
        return this == AIR;
    }

    public boolean isOpaque() {
        return solid && !transparent;
    }

    /**
     * Get BlockType from its ordinal ID.
     */
    public static BlockType fromId(int id) {
        if (id < 0 || id >= COUNT) return AIR;
        return VALUES[id];
    }
}
