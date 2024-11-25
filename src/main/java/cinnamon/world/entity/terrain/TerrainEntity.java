package cinnamon.world.entity.terrain;

import cinnamon.registry.EntityRegistry;
import cinnamon.utils.Resource;
import cinnamon.world.entity.Entity;

import java.util.UUID;

public abstract class TerrainEntity extends Entity {

    public TerrainEntity(UUID uuid, Resource model) {
        super(uuid, model);
    }

    @Override
    public EntityRegistry getType() {
        return EntityRegistry.TERRAIN;
    }
}
