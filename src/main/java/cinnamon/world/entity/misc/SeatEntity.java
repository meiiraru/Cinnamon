package cinnamon.world.entity.misc;

import cinnamon.registry.EntityRegistry;
import cinnamon.world.entity.Entity;

import java.util.UUID;

public class SeatEntity extends Entity {

    public SeatEntity(UUID uuid) {
        super(uuid, null);
    }

    @Override
    public void calculateBounds() {
        this.aabb.set(getTransform().getPos());
    }

    @Override
    protected void removeRider(Entity e) {
        super.removeRider(e);
        if (getRiders().isEmpty())
            this.remove();
    }

    @Override
    public EntityRegistry getType() {
        return EntityRegistry.SEAT;
    }
}
