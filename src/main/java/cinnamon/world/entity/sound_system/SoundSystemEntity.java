package cinnamon.world.entity.sound_system;

import cinnamon.registry.EntityRegistry;
import cinnamon.utils.Resource;
import cinnamon.world.entity.Entity;

import java.util.UUID;

public abstract class SoundSystemEntity extends Entity {

    public SoundSystemEntity(UUID uuid, Resource model) {
        super(uuid, model);
    }

    @Override
    public EntityRegistry getType() {
        return EntityRegistry.SOUND_SYSTEM;
    }
}
