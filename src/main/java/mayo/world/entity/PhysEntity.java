package mayo.world.entity;

import mayo.render.Model;
import mayo.world.World;
import org.joml.Vector3f;

public abstract class PhysEntity extends Entity {

    protected final Vector3f
            velocity = new Vector3f(),
            acceleration = new Vector3f();

    public PhysEntity(Model model, World world) {
        super(model, world);
    }

    @Override
    protected void tickPhysics() {
        this.oRot.set(rot);
        this.velocity.set(pos).sub(oPos);

        applyForces();

        this.oPos.set(pos);
        moveTo(
                pos.x + velocity.x + acceleration.x,
                pos.y + velocity.y + acceleration.y,
                pos.z + velocity.z + acceleration.z
        );
        this.acceleration.set(0);
    }

    protected void applyForces() {}

    public void accelerate(float left, float up, float forwards) {
        Vector3f vec = new Vector3f(-left, up, -forwards);

        vec.rotateX((float) Math.toRadians(-rot.x));
        vec.rotateY((float) Math.toRadians(-rot.y));

        this.acceleration.add(vec);
    }

    public void setAcceleration(Vector3f vec) {
        setAcceleration(vec.x, vec.y, vec.z);
    }

    public void setAcceleration(float x, float y, float z) {
        this.acceleration.set(x, y, z);
    }

    public Vector3f getAcceleration() {
        return acceleration;
    }

    public Vector3f getVelocity() {
        return velocity;
    }
}
