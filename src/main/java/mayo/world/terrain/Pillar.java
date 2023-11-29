package mayo.world.terrain;

import mayo.registry.TerrainRegistry;

public class Pillar extends Terrain {
    @Override
    public TerrainRegistry getType() {
        return TerrainRegistry.PILLAR;
    }
}
