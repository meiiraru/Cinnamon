package cinnamon.world.entity.terrain;

import cinnamon.registry.TerrainEntityRegistry;

import java.util.UUID;

public class DiscoBall extends TerrainEntity {

    public DiscoBall(UUID uuid) {
        super(uuid, TerrainEntityRegistry.DISCO_BALL.resource);
    }
}
