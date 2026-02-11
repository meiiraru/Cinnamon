package cinnamon.world.voxel;

/**
 * Procedural terrain generator using simplex noise with biomes and trees.
 * <p>
 * Biome system uses two large-scale noise fields (temperature, moisture) to determine
 * biome type per column: Plains, Forest, Desert, Tundra.
 * Each biome controls surface blocks, tree density, terrain amplitude, and water behavior.
 * <p>
 * Trees are placed deterministically using a hash function so every chunk can independently
 * check nearby tree positions (handling cross-chunk canopies).
 * <p>
 * Uses multi-octave fractal noise for natural-looking rolling hills,
 * with 3D noise for cave carving.
 */
public final class VoxelWorldGenerator {

    // Terrain generation parameters
    private static final int SEA_LEVEL = 32;
    private static final int BASE_HEIGHT = 28;
    private static final int SNOW_LEVEL = 55;

    // Noise parameters for height map
    private static final int HEIGHT_OCTAVES = 5;
    private static final double HEIGHT_PERSISTENCE = 0.5;
    private static final double HEIGHT_SCALE = 0.008;

    // Noise parameters for detail (hills and valleys)
    private static final int DETAIL_OCTAVES = 3;
    private static final double DETAIL_PERSISTENCE = 0.4;
    private static final double DETAIL_SCALE = 0.03;

    // Cave parameters
    private static final double CAVE_SCALE = 0.04;
    private static final double CAVE_THRESHOLD = 0.55;

    // Biome noise parameters (large scale for smooth biome transitions)
    private static final double BIOME_SCALE = 0.002;
    private static final int BIOME_OCTAVES = 3;
    private static final double BIOME_PERSISTENCE = 0.5;

    // Tree parameters
    private static final int TREE_CHECK_RADIUS = 4; // check this many blocks outside chunk for cross-boundary trees
    private static final int TREE_TRUNK_MIN = 4;
    private static final int TREE_TRUNK_MAX = 6;
    private static final int TREE_CANOPY_RADIUS = 2;

    // Biome types
    private enum Biome {
        PLAINS,     // Grass, gentle hills, scattered trees
        FOREST,     // Dense trees, moderate elevation
        DESERT,     // Sand, flat, no water fill, no trees
        TUNDRA      // Snow, sparse trees, frozen water
    }

    private VoxelWorldGenerator() {}

    /**
     * Generate terrain for a chunk at the given grid position.
     */
    public static void generateChunk(VoxelChunk chunk) {
        int worldX = chunk.cx * VoxelChunk.SIZE;
        int worldY = chunk.cy * VoxelChunk.SIZE;
        int worldZ = chunk.cz * VoxelChunk.SIZE;

        // First pass: basic terrain
        for (int lx = 0; lx < VoxelChunk.SIZE; lx++) {
            for (int lz = 0; lz < VoxelChunk.SIZE; lz++) {
                int wx = worldX + lx;
                int wz = worldZ + lz;

                Biome biome = getBiome(wx, wz);
                int terrainHeight = getTerrainHeight(wx, wz, biome);

                for (int ly = 0; ly < VoxelChunk.SIZE; ly++) {
                    int wy = worldY + ly;

                    BlockType type = getBlockAt(wx, wy, wz, terrainHeight, biome);
                    if (type != BlockType.AIR) {
                        chunk.setBlockFast(lx, ly, lz, type);
                    }
                }
            }
        }

        // Second pass: place trees (check surrounding area for tree trunks that may overlap this chunk)
        placeTreesInChunk(chunk, worldX, worldY, worldZ);

        chunk.finishBulkSet();
    }

    // ---- Biome System ---- //

    /**
     * Get biome at a world XZ position using temperature and moisture noise.
     */
    private static Biome getBiome(int wx, int wz) {
        // Temperature: -1 = cold, +1 = hot
        double temperature = SimplexNoise.fbm2D(wx + 5000, wz + 5000, BIOME_OCTAVES, BIOME_PERSISTENCE, BIOME_SCALE);
        // Moisture: -1 = dry, +1 = wet
        double moisture = SimplexNoise.fbm2D(wx + 10000, wz + 10000, BIOME_OCTAVES, BIOME_PERSISTENCE, BIOME_SCALE);

        if (temperature > 0.3) {
            // Hot: desert if dry, plains if wet
            return moisture < 0.0 ? Biome.DESERT : Biome.PLAINS;
        } else if (temperature < -0.3) {
            // Cold: tundra
            return Biome.TUNDRA;
        } else {
            // Moderate: forest if wet, plains if dry
            return moisture > -0.1 ? Biome.FOREST : Biome.PLAINS;
        }
    }

    /**
     * Get terrain height at a world column, adjusted by biome.
     */
    private static int getTerrainHeight(int wx, int wz, Biome biome) {
        double heightNoise = SimplexNoise.fbm2D(wx, wz, HEIGHT_OCTAVES, HEIGHT_PERSISTENCE, HEIGHT_SCALE);
        double detailNoise = SimplexNoise.fbm2D(wx + 1000, wz + 1000, DETAIL_OCTAVES, DETAIL_PERSISTENCE, DETAIL_SCALE);

        // Biome-specific height range
        double heightRange = switch (biome) {
            case PLAINS -> 30;
            case FOREST -> 35;
            case DESERT -> 15;  // Flatter
            case TUNDRA -> 25;
        };

        return BASE_HEIGHT + (int) (heightNoise * heightRange * 0.5 + detailNoise * 6);
    }

    /**
     * Determine the block type at a world position.
     */
    private static BlockType getBlockAt(int wx, int wy, int wz, int terrainHeight, Biome biome) {
        if (wy > terrainHeight) {
            // Above terrain — air or water/ice if below sea level
            if (wy <= SEA_LEVEL) {
                if (biome == Biome.DESERT) return BlockType.AIR; // No water in deserts
                if (biome == Biome.TUNDRA && wy == SEA_LEVEL) return BlockType.MARBLE; // Ice surface (using marble as ice)
                return BlockType.WATER;
            }
            return BlockType.AIR;
        }

        // At or below terrain surface
        int depth = terrainHeight - wy;

        // Cave carving using 3D noise
        if (wy > 2 && wy < terrainHeight - 1) {
            double caveNoise = Math.abs(SimplexNoise.noise3D(wx * CAVE_SCALE, wy * CAVE_SCALE, wz * CAVE_SCALE));
            if (caveNoise > CAVE_THRESHOLD) {
                return BlockType.AIR; // carved cave
            }
        }

        // Surface block
        if (depth == 0) {
            return getSurfaceBlock(wx, wy, wz, terrainHeight, biome);
        }

        // Near-surface blocks (1-3 below surface)
        if (depth <= 3) {
            return getSubSurfaceBlock(biome, wy, terrainHeight);
        }

        // Deep blocks
        if (depth > 20) {
            double oreNoise = SimplexNoise.noise3D(wx * 0.1, wy * 0.1, wz * 0.1);
            if (oreNoise > 0.75) return BlockType.GOLD;
            if (oreNoise > 0.65) return BlockType.IRON;
        }

        return BlockType.STONE;
    }

    private static BlockType getSurfaceBlock(int wx, int wy, int wz, int terrainHeight, Biome biome) {
        return switch (biome) {
            case PLAINS -> {
                if (wy >= SNOW_LEVEL) yield BlockType.SNOW;
                if (wy <= SEA_LEVEL + 2 && terrainHeight <= SEA_LEVEL + 3) yield BlockType.SAND;
                yield BlockType.GRASS;
            }
            case FOREST -> {
                if (wy >= SNOW_LEVEL) yield BlockType.SNOW;
                if (wy <= SEA_LEVEL + 2 && terrainHeight <= SEA_LEVEL + 3) yield BlockType.SAND;
                // Some forest floors have moss
                double mossNoise = SimplexNoise.noise2D(wx * 0.1, wz * 0.1);
                yield mossNoise > 0.5 ? BlockType.MOSS : BlockType.GRASS;
            }
            case DESERT -> BlockType.SAND;
            case TUNDRA -> {
                if (wy <= SEA_LEVEL + 2 && terrainHeight <= SEA_LEVEL + 3) yield BlockType.SNOW;
                yield BlockType.SNOW;
            }
        };
    }

    private static BlockType getSubSurfaceBlock(Biome biome, int wy, int terrainHeight) {
        return switch (biome) {
            case PLAINS, FOREST -> {
                if (wy <= SEA_LEVEL + 2 && terrainHeight <= SEA_LEVEL + 3) yield BlockType.SAND;
                yield BlockType.DIRT;
            }
            case DESERT -> BlockType.SAND;
            case TUNDRA -> BlockType.DIRT;
        };
    }

    // ---- Tree Generation ---- //

    /**
     * Place trees in a chunk by checking all potential tree positions that could affect this chunk.
     * Trees are placed deterministically based on a hash of their trunk position,
     * so any chunk can independently determine if a nearby tree's blocks fall within it.
     */
    private static void placeTreesInChunk(VoxelChunk chunk, int worldX, int worldY, int worldZ) {
        // Scan area includes a margin for tree canopies that may extend into this chunk
        int margin = TREE_CHECK_RADIUS + TREE_CANOPY_RADIUS;
        int scanMinX = worldX - margin;
        int scanMaxX = worldX + VoxelChunk.SIZE + margin;
        int scanMinZ = worldZ - margin;
        int scanMaxZ = worldZ + VoxelChunk.SIZE + margin;

        for (int tx = scanMinX; tx < scanMaxX; tx++) {
            for (int tz = scanMinZ; tz < scanMaxZ; tz++) {
                // Deterministic tree check: is there a tree trunk at (tx, tz)?
                if (!isTreePosition(tx, tz)) continue;

                Biome biome = getBiome(tx, tz);
                if (!canHaveTree(biome)) continue;

                // Check tree density for this biome
                if (!passesTreeDensity(tx, tz, biome)) continue;

                int terrainHeight = getTerrainHeight(tx, tz, biome);

                // Don't place trees underwater or on very flat/low terrain
                if (terrainHeight <= SEA_LEVEL) continue;
                // Don't place trees on snow-capped peaks
                if (terrainHeight >= SNOW_LEVEL) continue;

                // Check surface is suitable (not sand beach)
                BlockType surface = getSurfaceBlock(tx, terrainHeight, tz, terrainHeight, biome);
                if (surface == BlockType.SAND) continue;

                int trunkHeight = getTreeTrunkHeight(tx, tz);
                int treeTop = terrainHeight + trunkHeight + 1;

                // Place trunk and canopy blocks that fall within this chunk
                placeTree(chunk, worldX, worldY, worldZ, tx, terrainHeight + 1, tz, trunkHeight, biome);
            }
        }
    }

    /**
     * Deterministic check if a tree should be at this XZ position.
     * Uses a hash to create a regular-ish spacing of ~5-7 blocks between trees.
     */
    private static boolean isTreePosition(int wx, int wz) {
        // Grid-based with jitter: trees can only spawn on a 5x5 grid with hash-based selection
        int gridX = Math.floorDiv(wx, 5);
        int gridZ = Math.floorDiv(wz, 5);
        long hash = hashPosition(gridX, gridZ);
        int candidateX = gridX * 5 + (int) ((hash & 0xF) % 5);
        int candidateZ = gridZ * 5 + (int) (((hash >> 4) & 0xF) % 5);
        return wx == candidateX && wz == candidateZ;
    }

    private static boolean canHaveTree(Biome biome) {
        return biome != Biome.DESERT; // Trees don't grow in deserts
    }

    private static boolean passesTreeDensity(int wx, int wz, Biome biome) {
        long hash = hashPosition(wx * 7, wz * 13);
        double density = (hash & 0xFF) / 255.0;
        double threshold = switch (biome) {
            case FOREST -> 0.15;  // Dense: 85% of grid positions get trees
            case PLAINS -> 0.75;  // Sparse: 25% of grid positions
            case TUNDRA -> 0.85;  // Very sparse: 15%
            case DESERT -> 1.0;   // Never (caught by canHaveTree anyway)
        };
        return density >= threshold;
    }

    private static int getTreeTrunkHeight(int wx, int wz) {
        long hash = hashPosition(wx * 3 + 7, wz * 3 + 13);
        return TREE_TRUNK_MIN + (int) ((hash & 0x7) % (TREE_TRUNK_MAX - TREE_TRUNK_MIN + 1));
    }

    /**
     * Place a single tree's blocks into the chunk if they overlap.
     */
    private static void placeTree(VoxelChunk chunk, int chunkWorldX, int chunkWorldY, int chunkWorldZ,
                                   int trunkX, int trunkBaseY, int trunkZ, int trunkHeight, Biome biome) {
        int chunkMaxX = chunkWorldX + VoxelChunk.SIZE;
        int chunkMaxY = chunkWorldY + VoxelChunk.SIZE;
        int chunkMaxZ = chunkWorldZ + VoxelChunk.SIZE;

        BlockType leafType = (biome == Biome.TUNDRA) ? BlockType.SNOW : BlockType.LEAVES;

        // Place trunk
        for (int y = trunkBaseY; y < trunkBaseY + trunkHeight; y++) {
            if (trunkX >= chunkWorldX && trunkX < chunkMaxX &&
                y >= chunkWorldY && y < chunkMaxY &&
                trunkZ >= chunkWorldZ && trunkZ < chunkMaxZ) {
                int lx = trunkX - chunkWorldX;
                int ly = y - chunkWorldY;
                int lz = trunkZ - chunkWorldZ;
                // Only place trunk if the position is currently air
                if (chunk.getBlock(lx, ly, lz).isAir()) {
                    chunk.setBlockFast(lx, ly, lz, BlockType.LOG);
                }
            }
        }

        // Place canopy (sphere-ish shape around the top of the trunk)
        int canopyBaseY = trunkBaseY + trunkHeight - 2;
        int canopyTopY = trunkBaseY + trunkHeight + 1;
        int canopyR = TREE_CANOPY_RADIUS;

        for (int cy = canopyBaseY; cy <= canopyTopY; cy++) {
            // Reduce radius at bottom and top of canopy
            int layerR = canopyR;
            if (cy == canopyBaseY || cy == canopyTopY) layerR = canopyR - 1;
            if (layerR < 0) layerR = 0;

            for (int cx = trunkX - layerR; cx <= trunkX + layerR; cx++) {
                for (int cz = trunkZ - layerR; cz <= trunkZ + layerR; cz++) {
                    // Skip corners for a rounder shape
                    int dx = cx - trunkX;
                    int dz = cz - trunkZ;
                    if (Math.abs(dx) == layerR && Math.abs(dz) == layerR) continue;

                    // Check if in chunk bounds
                    if (cx >= chunkWorldX && cx < chunkMaxX &&
                        cy >= chunkWorldY && cy < chunkMaxY &&
                        cz >= chunkWorldZ && cz < chunkMaxZ) {
                        int lx = cx - chunkWorldX;
                        int ly = cy - chunkWorldY;
                        int lz = cz - chunkWorldZ;
                        // Only place leaves in air (don't overwrite trunk or terrain)
                        BlockType existing = chunk.getBlock(lx, ly, lz);
                        if (existing.isAir() || existing == BlockType.WATER) {
                            chunk.setBlockFast(lx, ly, lz, leafType);
                        }
                    }
                }
            }
        }
    }

    /**
     * Simple deterministic hash for world coordinates.
     */
    private static long hashPosition(int x, int z) {
        long h = x * 374761393L + z * 668265263L;
        h = (h ^ (h >> 13)) * 1274126177L;
        h = h ^ (h >> 16);
        return h & 0xFFFFFFFFL; // ensure positive
    }
}
