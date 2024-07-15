package cinnamon.world.terrain;

import cinnamon.registry.TerrainRegistry;

public class Sphere extends Terrain {
    @Override
    public TerrainRegistry getType() {
        return TerrainRegistry.SPHERE;
    }
}
