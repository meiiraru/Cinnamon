package cinnamon.world.entity.terrain;

import cinnamon.registry.TerrainEntityRegistry;

import java.util.UUID;

public class DiscoFloor extends TerrainEntity {

    public DiscoFloor(UUID uuid) {
        super(uuid, TerrainEntityRegistry.DISCO_FLOOR.resource);
    }
}
