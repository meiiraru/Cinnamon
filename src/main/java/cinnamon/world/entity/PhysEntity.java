package cinnamon.world.entity;

import cinnamon.utils.AABB;
import cinnamon.utils.Resource;
import cinnamon.world.Mask;
import cinnamon.world.collisions.CollisionDetector;
import cinnamon.world.collisions.CollisionResolver;
import cinnamon.world.collisions.CollisionResult;
import cinnamon.world.terrain.Terrain;
import org.joml.Math;
import org.joml.Vector3f;

import java.util.List;
import java.util.UUID;

public abstract class PhysEntity extends Entity {

    protected final Vector3f
            motion = new Vector3f(),
            impulse = new Vector3f();

    protected boolean onGround;
    protected float gravity = 1f;
    protected Mask
            entityCollisionMask = new Mask(),
            terrainCollisionMask = new Mask();

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

        //check for terrain collisions
        Vector3f toMove = tickTerrainCollisions(aabb, motion);

        //entity collisions
        tickEntityCollisions(aabb, toMove);

        //move entity
        if (toMove.lengthSquared() > 0f)
            moveTo(pos.x + toMove.x, pos.y + toMove.y, pos.z + toMove.z);

        //decrease motion
        motionFallout();
    }

    // -- movement -- //

    protected void applyForces() {
        //gravity
        this.motion.y -= world.gravity * getGravity();
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

    protected Vector3f tickTerrainCollisions(AABB aabb, Vector3f motion) {
        //early exit
        if (motion.lengthSquared() < 1e-9f)
            return new Vector3f();

        //prepare variables
        Vector3f pos = aabb.getCenter();
        Vector3f inflate = aabb.getDimensions().mul(0.5f);
        Vector3f toMove = new Vector3f(motion);
        boolean ground = false;

        //get terrain collisions
        List<Terrain> terrains = getWorld().getTerrains(new AABB(aabb).expand(toMove));

        //try to resolve collisions in max 3 steps
        for (int i = 0; i < 3; i++) {
            //find the closest collision
            CollisionResult collision = null;

            for (Terrain terrain : terrains) {
                if (!getTerrainCollisionMask().test(terrain.getCollisionMask()))
                    continue;

                for (AABB terrainBB : terrain.getPreciseAABB()) {
                    //inflate terrain BB by the entity half-dimensions
                    AABB inflatedBB = new AABB(terrainBB).inflate(inflate);

                    //check for collision along the motion ray
                    CollisionResult result = CollisionDetector.collisionRay(inflatedBB, pos, toMove);
                    if (result != null && (collision == null || result.near() < collision.near()))
                        collision = result;
                }
            }

            //no collision found - exit loop
            if (collision == null)
                break;

            //set ground state when on a floor
            if (!ground && collision.normal().y > 0)
                ground = true;

            //resolve the collision
            resolveCollision(collision, toMove);

            //stop if remaining movement is too small
            if (toMove.lengthSquared() < 1e-9f) {
                toMove.set(0);
                break;
            }
        }

        this.onGround = ground;
        return toMove;
    }

    protected void resolveCollision(CollisionResult collision, Vector3f totalMove) {
        CollisionResolver.slide(collision, getMotion(), totalMove);
    }

    // -- entity collisions -- //

    protected void tickEntityCollisions(AABB aabb, Vector3f toMove) {
        Vector3f pos = aabb.getCenter();
        Vector3f inflate = aabb.getDimensions().mul(0.5f);

        for (Entity entity : getWorld().getEntities(new AABB(aabb).expand(toMove))) {
            if (!(entity instanceof PhysEntity physEntity) || physEntity == this || physEntity.isRemoved() || !getEntityCollisionMask().test(physEntity.getEntityCollisionMask()))
                continue;

            AABB temp = new AABB(physEntity.getAABB()).inflate(inflate);
            CollisionResult result = CollisionDetector.collisionRay(temp, pos, toMove);
            if (result != null)
                collide(physEntity, result, toMove);
        }
    }

    protected Vector3f checkEntityCollision(PhysEntity entity) {
        //get AABB
        AABB other = entity.getAABB();

        //calculate collision
        return new Vector3f(
                aabb.getXOverlap(other),
                aabb.getYOverlap(other),
                aabb.getZOverlap(other)
        );
    }

    protected void collide(PhysEntity entity, CollisionResult result, Vector3f toMove) {}

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
        this.impulse.rotateX(Math.toRadians(-rot.x));
        this.impulse.rotateY(Math.toRadians(-rot.y));
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

    public void setImpulse(Vector3f vec) {
        this.setImpulse(vec.x, vec.y, vec.z);
    }

    public void setImpulse(float x, float y, float z) {
        this.impulse.set(x, y, z);
    }

    public Vector3f getImpulse() {
        return impulse;
    }

    public boolean isOnGround() {
        return onGround;
    }

    public void knockback(Vector3f dir, float force) {
        setMotion(motion.x + dir.x * force, motion.y + dir.y * force, motion.z + dir.z * force);
    }

    public void setGravity(float gravity) {
        this.gravity = gravity;
    }

    public float getGravity() {
        return gravity;
    }

    public Mask getTerrainCollisionMask() {
        return terrainCollisionMask;
    }

    public Mask getEntityCollisionMask() {
        return entityCollisionMask;
    }
}
