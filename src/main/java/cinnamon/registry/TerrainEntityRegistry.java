package cinnamon.registry;

import cinnamon.utils.Resource;

public enum TerrainEntityRegistry {
    SPEAKER("models/entities/terrain/speaker/speaker.obj"),
    FLOOR_LIGHT("models/entities/terrain/floor_light/floor_light.obj"),
    DISCO_BALL("models/entities/terrain/disco_ball/disco_ball.obj"),
    PARTICLE_SPAWNER("models/entities/terrain/particle_spawner/particle_spawner.obj"),
    DISCO_FLOOR("models/entities/terrain/disco_floor/floor.obj");

    public final Resource resource;

    TerrainEntityRegistry(String path) {
        this.resource = new Resource(path);
    }
}
