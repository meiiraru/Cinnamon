package cinnamon.world.entity.terrain;

import cinnamon.utils.Resource;

import java.util.UUID;

public class DiscoBall extends TerrainEntity {

    private static final Resource MODEL = new Resource("models/entities/terrain/disco_ball/disco_ball.obj");

    public DiscoBall(UUID uuid) {
        super(uuid, MODEL);
    }
}
