package cinnamon.world.entity.sound_system;

import cinnamon.registry.SoundSystemEntityRegistry;

import java.util.UUID;

public class DiscoFloor extends SoundSystemEntity {

    public DiscoFloor(UUID uuid) {
        super(uuid, SoundSystemEntityRegistry.DISCO_FLOOR.resource);
    }
}
