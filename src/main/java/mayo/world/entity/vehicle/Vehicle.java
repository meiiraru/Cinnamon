package mayo.world.entity.vehicle;

import mayo.render.Model;
import mayo.world.entity.Entity;
import mayo.world.entity.PhysEntity;
import mayo.world.entity.living.LivingEntity;

public abstract class Vehicle extends PhysEntity {

    private final int maxRiders;

    public Vehicle(Model model, int maxRiders) {
        super(model);
        this.maxRiders = maxRiders;
    }

    @Override
    public void move(float left, float up, float forwards) {
        if (riding != null) {
            riding.move(left, up, forwards);
            return;
        }

        float l = Math.signum(left);
        float f = Math.signum(forwards);

        this.move.set(l, 0, -f);

        if (move.lengthSquared() > 1)
            move.normalize();
        move.mul(getMoveSpeed());

        //move the entity in facing direction
        this.move.rotateY((float) Math.toRadians(-rot.y));
    }

    @Override
    public void rotateTo(float pitch, float yaw) {
        super.rotateTo(0, yaw);
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
