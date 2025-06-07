package cinnamon.world.entity;

import cinnamon.utils.AABB;
import cinnamon.utils.Resource;
import cinnamon.world.collisions.CollisionDetector;
import cinnamon.world.collisions.CollisionResolver;
import cinnamon.world.collisions.CollisionResult;
import org.joml.Vector3f;

import java.util.List;
import java.util.UUID;

public abstract class PhysEntity extends Entity {

    protected final Vector3f
            motion = new Vector3f(),
            impulse = new Vector3f();

    protected boolean onGround;

    public PhysEntity(UUID uuid, Resource model) {
        super(uuid, model);
    }

    public void tick() {
        super.tick();

        //calculate physics only when not riding
        if (!this.isRiding()) {
            tickPhysics();
        } else {
            setMotion(0, 0, 0);
        }
    }

    protected void tickPhysics() {
        //apply ambient forces
        applyForces();

        //apply my current desired movement into my motion
        applyImpulse();
        onGround = false;

        //check for terrain collisions
        Vector3f toMove = tickTerrainCollisions();

        //entity collisions
        tickEntityCollisions(toMove);

        //move entity
        if (toMove.lengthSquared() > 0f)
            moveTo(pos.x + toMove.x, pos.y + toMove.y, pos.z + toMove.z);

        //decrease motion
        motionFallout();
    }

    // -- movement -- //

    protected void applyForces() {
        //gravity
        this.motion.y -= world.gravity;
    }

    protected void applyImpulse() {
        this.motion.add(impulse.mul(onGround ? 1 : 0.125f));
        this.impulse.set(0);
    }

    protected void motionFallout() {
        //decrease motion (fake friction/resistance)

        //air
        this.motion.mul(0.91f, 0.98f, 0.91f);

        //ground
        if (this.onGround)
            this.motion.mul(0.5f, 1f, 0.5f);
    }

    // -- terrain collisions -- //

    protected Vector3f tickTerrainCollisions() {
        //early exit
        if (motion.lengthSquared() < 0.001f)
            return motion.mul(0, new Vector3f());

        //prepare variables
        Vector3f pos = aabb.getCenter();
        Vector3f inflate = aabb.getDimensions().mul(0.5f);
        Vector3f toMove = new Vector3f(motion);

        //get terrain collisions
        List<AABB> terrain = world.getTerrainCollisions(new AABB(aabb).expand(toMove));

        //try to resolve collisions in max 3 steps
        for (int i = 0; i < 3; i++) {
            CollisionResult collision = null;

            for (AABB terrainBB : terrain) {
                //update bb to include this source's bb
                AABB temp = new AABB(terrainBB).inflate(inflate);

                //check collision
                CollisionResult result = CollisionDetector.collisionRay(temp, pos, toMove);
                if (result != null && (collision == null || collision.near() > result.near())) {
                    collision = result;
                }
            }

            //resolve collision
            if (collision != null) {
                //set ground state
                if (collision.normal().y > 0)
                    this.onGround = true;

                //resolve collision
                resolveCollision(collision, motion, toMove);
            } else {
                //no collision detected
                break;
            }
        }

        return toMove;
    }

    protected void resolveCollision(CollisionResult collision, Vector3f motion, Vector3f move) {
        CollisionResolver.slide(collision, motion, move);
    }

    // -- entity collisions -- //

    protected void tickEntityCollisions(Vector3f toMove) {
        for (Entity entity : world.getEntities(new AABB(aabb).expand(toMove)))
            if (entity != this && !entity.isRemoved())
                collide(entity);
    }

    protected Vector3f checkEntityCollision(Entity entity) {
        //get AABB
        AABB other = entity.getAABB();

        //calculate collision
        return new Vector3f(
                aabb.getXOverlap(other),
                aabb.getYOverlap(other),
                aabb.getZOverlap(other)
        );
    }

    protected void collide(Entity entity) {}

    // -- movement logic -- //

    @Override
    public void impulse(float left, float up, float forwards) {
        if (riding != null) {
            riding.impulse(left, up, forwards);
            return;
        }

        float l = Math.signum(left);
        float u = Math.signum(up);
        float f = Math.signum(forwards);

        this.impulse.set(l, u, -f);
        if (impulse.lengthSquared() > 1)
            impulse.normalize();

        impulse.mul(getMoveSpeed());

        //move the entity in facing direction
        this.impulse.rotateX((float) Math.toRadians(-rot.x));
        this.impulse.rotateY((float) Math.toRadians(-rot.y));
    }

    protected float getMoveSpeed() {
        return 0.15f;
    }

    protected float getPushForce() {
        return 0.015f;
    }

    public void setMotion(Vector3f vec) {
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

    public void knockback(Vector3f dir, float force) {
        setMotion(motion.x + dir.x * force, motion.y + dir.y * force, motion.z + dir.z * force);
    }
}
