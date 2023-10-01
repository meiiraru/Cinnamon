package mayo.world.entity;

import mayo.render.Model;
import mayo.utils.AABB;
import mayo.world.World;
import org.joml.Vector3f;

import java.util.List;

public abstract class PhysEntity extends Entity {

    protected final Vector3f
            velocity = new Vector3f(),
            acceleration = new Vector3f();

    protected boolean onGround;

    public PhysEntity(Model model, World world) {
        super(model, world);
    }

    @Override
    protected void tickPhysics() {
        this.oRot.set(rot);
        this.velocity.set(pos).sub(oPos);
        this.oPos.set(pos);

        applyForces();

        //move the entity using the motion
        Vector3f collision = checkCollisions(acceleration);

        //update on ground state
        this.onGround = acceleration.y != collision.y && acceleration.y < 0f;

        //stop the motion on collision - or bounce :)
        if (acceleration.x != collision.x) this.acceleration.x *= -1f;
        if (acceleration.y != collision.y) this.acceleration.y *= -1f;
        if (acceleration.z != collision.z) this.acceleration.z *= -1f;

        //move position
        moveTo(pos.x + collision.x, pos.y + collision.y, pos.z + collision.z);

        //decrease motion
        this.acceleration.mul(0.98f, 0.98f, 0.98f);

        //decrease motion on ground
        if (this.onGround)
            this.acceleration.mul(0.85f, 0.98f, 0.85f);
    }

    protected void applyForces() {
        //thats gravity and stuff:tm:
        this.acceleration.y -= world.gravity;
    }

    public void moveRelative(float left, float up, float forwards) {
        if (up > 0)
            this.acceleration.y = this.onGround ? 0.64f : 0.4f;

        float distance = left * left + forwards * forwards;
        //stop moving if too slow
        if (distance < 0.01f)
            return;

        //apply speed to relative movement
        distance = 0.15f / (float) Math.sqrt(distance);
        left *= distance;
        forwards *= distance;

        //move the entity in facing direction
        this.acceleration.set(left, this.acceleration.y, -forwards);
        this.acceleration.rotateY((float) Math.toRadians(-rot.y));
    }

    protected Vector3f checkCollisions(Vector3f vec) {
        return checkCollisions(vec.x, vec.y, vec.z);
    }

    protected Vector3f checkCollisions(float x, float y, float z) {
        //get terrain collisions
        AABB tempBB = new AABB(aabb);
        List<AABB> collisions = world.getTerrainCollisions(new AABB(aabb).expand(x, y, z));

        //check for Y collision
        for (AABB aabb : collisions)
            y = aabb.clipYCollide(tempBB, y);
        tempBB.translate(0f, y, 0f);

        //check for X collision
        for (AABB aabb : collisions)
            x = aabb.clipXCollide(tempBB, x);
        tempBB.translate(x, 0f, 0f);

        //check for Z collision
        for (AABB aabb : collisions)
            z = aabb.clipZCollide(tempBB, z);
        tempBB.translate(0f, 0f, z);

        return new Vector3f(x, y, z);
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

    public boolean isOnGround() {
        return onGround;
    }
}
