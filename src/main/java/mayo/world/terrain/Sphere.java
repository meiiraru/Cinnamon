package mayo.world.terrain;

import mayo.registry.TerrainRegistry;

public class Sphere extends Terrain {
    @Override
    public TerrainRegistry getType() {
        return TerrainRegistry.SPHERE;
    }
}
