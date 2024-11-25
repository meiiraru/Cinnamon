package cinnamon.world.entity.terrain;

import cinnamon.registry.TerrainEntityRegistry;
import cinnamon.utils.Resource;

import java.util.UUID;

public class Speaker extends TerrainEntity {

    public Speaker(UUID uuid) {
        super(uuid, TerrainEntityRegistry.SPEAKER.resource);
    }
}
