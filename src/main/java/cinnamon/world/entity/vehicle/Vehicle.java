package cinnamon.world.entity.vehicle;

import cinnamon.utils.Resource;
import cinnamon.utils.Rotation;
import cinnamon.world.entity.Entity;
import cinnamon.world.entity.PhysEntity;
import cinnamon.world.entity.living.LivingEntity;
import org.joml.Vector3f;

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
    public boolean onUse(LivingEntity source) {
        return this.addRider(source) || super.onUse(source);
    }

    @Override
    public boolean addRider(Entity e) {
        if (this.riders.size() < maxRiders) {
            super.addRider(e);
            e.rotateTo(this.getRot());
            return true;
        }
        return false;
    }

    @Override
    protected void removeRider(Entity e) {
        super.removeRider(e);

        Vector3f pos = getPos();
        Vector3f dir = getLookDir().rotate(Rotation.Y.rotationDeg(90));
        dir.mul(getAABB().getWidth() * 0.5f + e.getAABB().getWidth() * 0.5f + 0.1f);
        e.moveTo(pos.x + dir.x, pos.y + 0.5f, pos.z + dir.z);
    }
}
