package cinnamon.world.entity.sound_system;

import cinnamon.registry.SoundSystemEntityRegistry;

import java.util.UUID;

public class Speaker extends SoundSystemEntity {

    public Speaker(UUID uuid) {
        super(uuid, SoundSystemEntityRegistry.SPEAKER.resource);
    }
}
