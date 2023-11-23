package mayo.world.entity.vehicle;

import mayo.render.Model;
import mayo.world.World;
import mayo.world.entity.Entity;
import mayo.world.entity.PhysEntity;
import mayo.world.entity.living.LivingEntity;
import org.joml.Vector3f;

public abstract class Vehicle extends PhysEntity {

    private final int maxRiders;

    public Vehicle(Model model, World world, int maxRiders) {
        super(model, world);
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
    protected void updateRiders() {
        if (riders.isEmpty())
            return;

        Vector3f riderPos = new Vector3f(pos.x, pos.y + getSeatHeight(), pos.z);

        for (Entity rider : riders)
            rider.moveTo(riderPos);
    }

    @Override
    public void onUse(LivingEntity source) {
        if (this.riders.size() < maxRiders) {
            source.rideEntity(this);
            source.rotateTo(this.getRot());
        }

        super.onUse(source);
    }

    public float getSeatHeight() {
        return aabb.getHeight();
    }
}
