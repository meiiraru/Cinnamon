package cinnamon.world.entity.sound_system;

import cinnamon.registry.SoundSystemEntityRegistry;

import java.util.UUID;

public class DiscoBall extends SoundSystemEntity {

    public DiscoBall(UUID uuid) {
        super(uuid, SoundSystemEntityRegistry.DISCO_BALL.resource);
    }
}
