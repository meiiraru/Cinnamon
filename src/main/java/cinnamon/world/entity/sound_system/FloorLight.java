package cinnamon.world.entity.sound_system;

import cinnamon.registry.SoundSystemEntityRegistry;

import java.util.UUID;

public class FloorLight extends SoundSystemEntity {

    public FloorLight(UUID uuid) {
        super(uuid, SoundSystemEntityRegistry.FLOOR_LIGHT.resource);
    }
}
