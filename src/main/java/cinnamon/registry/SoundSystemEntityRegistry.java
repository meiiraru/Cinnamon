package cinnamon.registry;

import cinnamon.utils.Resource;

public enum SoundSystemEntityRegistry {
    SPEAKER("models/entities/sound_system/speaker/speaker.obj"),
    FLOOR_LIGHT("models/entities/sound_system/floor_light/floor_light.obj"),
    DISCO_BALL("models/entities/sound_system/disco_ball/disco_ball.obj"),
    PARTICLE_SPAWNER("models/entities/sound_system/particle_spawner/particle_spawner.obj"),
    DISCO_FLOOR("models/entities/sound_system/disco_floor/floor.obj");

    public final Resource resource;

    SoundSystemEntityRegistry(String path) {
        this.resource = new Resource(path);
    }
}
