package cinnamon.world.entity.terrain;

import cinnamon.registry.TerrainEntityRegistry;

import java.util.UUID;

public class FloorLight extends TerrainEntity {

    public FloorLight(UUID uuid) {
        super(uuid, TerrainEntityRegistry.FLOOR_LIGHT.resource);
    }
}
