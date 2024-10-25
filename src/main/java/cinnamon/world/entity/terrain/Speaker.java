package cinnamon.world.entity.terrain;

import cinnamon.utils.Resource;

import java.util.UUID;

public class Speaker extends TerrainEntity {

    private static final Resource MODEL = new Resource("models/entities/terrain/speaker/speaker.obj");

    public Speaker(UUID uuid) {
        super(uuid, MODEL);
    }
}
