package cinnamon.world.entity.terrain;

import cinnamon.utils.Resource;

import java.util.UUID;

public class FloorLight extends TerrainEntity {

    private static final Resource MODEL = new Resource("models/entities/terrain/floor_light/floor_light.obj");

    public FloorLight(UUID uuid) {
        super(uuid, MODEL);
    }
}
