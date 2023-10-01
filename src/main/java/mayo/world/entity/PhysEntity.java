package mayo.world.entity;

import mayo.render.Model;
import mayo.utils.AABB;
import mayo.world.World;
import org.joml.Vector3f;

import java.util.List;

public abstract class PhysEntity extends Entity {

    protected final Vector3f motion = new Vector3f();

    protected boolean onGround;

    public PhysEntity(Model model, World world) {
        super(model, world);
    }

    @Override
    protected void tickPhysics() {
        super.tickPhysics();

        //apply forces like gravity:tm:
        applyForces();

        //move the entity using the motion
        Vector3f allowedMotion = checkCollisions(motion);

        //update on ground state
        this.onGround = motion.y != allowedMotion.y && motion.y < 0f;

        //resolve the collision
        resolveCollision(motion.x != allowedMotion.x, motion.y != allowedMotion.y, motion.z != allowedMotion.z);

        //move position
        moveTo(pos.x + allowedMotion.x, pos.y + allowedMotion.y, pos.z + allowedMotion.z);

        //decrease motion
        motionFallout();
    }

    protected void applyForces() {
        //that's gravity and stuff:tm:
        this.motion.y -= world.gravity;
    }

    protected void resolveCollision(boolean x, boolean y, boolean z) {
        //stop the motion
        if (x) this.motion.x = 0f;
        if (y) this.motion.y = 0f;
        if (z) this.motion.z = 0f;
    }

    protected void motionFallout() {
        //decrease motion (fake friction/resistance)

        //air
        this.motion.mul(0.91f, 0.98f, 0.91f);

        //ground
        if (this.onGround)
            this.motion.mul(0.5f, 1f, 0.5f);
    }

    @Override
    public void move(float left, float up, float forwards) {
        float distance = left * left + up * up + forwards * forwards;

        //stop moving if too slow
        if (distance < 0.01f)
            return;

        //apply speed to relative movement
        distance = getMoveSpeed() / (float) Math.sqrt(distance);
        left *= distance;
        up *= distance;
        forwards *= distance;

        //move the entity in facing direction
        this.motion.set(left, up, -forwards);
        this.motion.rotateX((float) Math.toRadians(-rot.x));
        this.motion.rotateY((float) Math.toRadians(-rot.y));
    }

    protected float getMoveSpeed() {
        return 0.2f;
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
        //tempBB.translate(0f, 0f, z);

        return new Vector3f(x, y, z);
    }

    public void setMotiom(Vector3f vec) {
        this.setMotion(vec.x, vec.y, vec.z);
    }

    public void setMotion(float x, float y, float z) {
        this.motion.set(x, y, z);
    }

    public Vector3f getMotion() {
        return motion;
    }

    public boolean isOnGround() {
        return onGround;
    }
}
