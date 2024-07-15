package cinnamon.world.terrain;

import cinnamon.registry.TerrainRegistry;

public class Box extends Terrain {
    @Override
    public TerrainRegistry getType() {
        return TerrainRegistry.BOX;
    }
}
