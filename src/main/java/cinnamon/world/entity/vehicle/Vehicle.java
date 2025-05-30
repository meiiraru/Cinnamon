package cinnamon.world.entity.vehicle;

import cinnamon.utils.Resource;
import cinnamon.world.entity.Entity;
import cinnamon.world.entity.PhysEntity;
import cinnamon.world.entity.living.LivingEntity;

import java.util.UUID;

public abstract class Vehicle extends PhysEntity {

    private final int maxRiders;

    public Vehicle(UUID uuid, Resource model, int maxRiders) {
        super(uuid, model);
        this.maxRiders = maxRiders;
    }

    @Override
    public void impulse(float left, float up, float forwards) {
        if (riding != null) {
            riding.impulse(left, up, forwards);
            return;
        }

        float l = Math.signum(left);
        float f = Math.signum(forwards);

        this.impulse.set(l, 0, -f);

        if (impulse.lengthSquared() > 1)
            impulse.normalize();
        impulse.mul(getMoveSpeed());

        //move the entity in facing direction
        this.impulse.rotateY((float) Math.toRadians(-rot.y));
    }

    @Override
    public void onUse(LivingEntity source) {
        this.addRider(source);
        super.onUse(source);
    }

    @Override
    public void addRider(Entity e) {
        if (this.riders.size() < maxRiders) {
            super.addRider(e);
            e.rotateTo(this.getRot());
        }
    }
}
