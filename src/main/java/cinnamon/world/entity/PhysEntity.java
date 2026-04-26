package cinnamon.world.entity;

import cinnamon.math.Maths;
import cinnamon.math.collision.AABB;
import cinnamon.math.collision.Collider;
import cinnamon.math.collision.Hit;
import cinnamon.math.collision.Resolution;
import cinnamon.utils.Resource;
import cinnamon.world.Mask;
import cinnamon.world.terrain.Terrain;
import org.joml.Math;
import org.joml.Vector3f;

import java.util.List;
import java.util.UUID;

public abstract class PhysEntity extends Entity {

    private static final Vector3f SMALL_IMPULSE = new Vector3f(0, -Maths.EPSILON, 0);

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
        if (impulse.y > 0)
            onGround = false;
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
        if (motion.lengthSquared() < Maths.SMALL_NUMBER)
            return new Vector3f();

        //standard terrain collision
        boolean[] groundState = {false};
        Vector3f standardMotionState = new Vector3f(motion);
        Vector3f standardMove = resolveTerrainCollisions(aabb, motion, standardMotionState, groundState);

        //try to step up if we hit a wall
        if (this.onGround && getStepHeight() > 0f) {
            boolean hitWall = Math.abs(standardMove.x) < Math.abs(motion.x) - Maths.SMALL_NUMBER || Math.abs(standardMove.z) < Math.abs(motion.z) - Maths.SMALL_NUMBER;
            boolean movedY = Math.abs(standardMove.y) > Maths.EPSILON;

            if (hitWall && !movedY) {
                Vector3f stepMove = stepUp(aabb, motion, standardMove);
                if (stepMove != null)
                    return stepMove;
            }
        }

        //no stepping, so apply standard states
        this.onGround = groundState[0];
        this.motion.set(standardMotionState);
        return standardMove;
    }

    protected Vector3f stepUp(AABB aabb, Vector3f motion, Vector3f standardMove) {
        //temporary vectors to check for terrain collisions
        Vector3f movement = new Vector3f();
        Vector3f currentMotion = new Vector3f();

        //step up
        movement.set(0, getStepHeight(), 0);
        Vector3f stepUpMove = resolveTerrainCollisions(aabb, movement, currentMotion, new boolean[]{false});

        //no upward movement
        if (stepUpMove.y <= 0)
            return null;

        //translate the AABB up by the step amount
        AABB elevatedAABB = new AABB(aabb).translate(stepUpMove);

        //apply remaining horizontal motion
        movement.set(motion.x, 0, motion.z);
        Vector3f forwardMotion = new Vector3f(movement);
        Vector3f stepForwardMove = resolveTerrainCollisions(elevatedAABB, movement, forwardMotion, new boolean[]{false});

        //snap back down to the floor if possible
        boolean[] stepGroundState = {false};
        AABB elevatedForwardAABB = elevatedAABB.translate(stepForwardMove);
        movement.set(0, -stepUpMove.y, 0);
        currentMotion.set(0, 0, 0);

        Vector3f stepDownMove = resolveTerrainCollisions(elevatedForwardAABB, movement, currentMotion, stepGroundState);

        //check if stepping sent us further than just walking forward horizontally
        float standardDistSq = standardMove.x * standardMove.x + standardMove.z * standardMove.z;
        float stepDistSq = stepForwardMove.x * stepForwardMove.x + stepForwardMove.z * stepForwardMove.z;
        if (stepDistSq <= standardDistSq)
            return null;

        //stepping was more successful, so apply its results
        this.onGround = stepGroundState[0] || standardMove.y == 0;

        //keep the forward sliding motion we calculated during the step forward
        this.motion.x = forwardMotion.x;
        this.motion.z = forwardMotion.z;

        //return the final translation of the step
        return currentMotion.set(stepUpMove).add(stepForwardMove).add(stepDownMove);
    }

    protected Vector3f resolveTerrainCollisions(AABB currentAABB, Vector3f movement, Vector3f currentMotion, boolean[] groundState) {
        //get terrain collisions
        Vector3f toMove = new Vector3f(movement);
        List<Terrain> terrains = getWorld().getTerrains(new AABB(currentAABB).expand(toMove));

        //try to resolve collisions with a step limit
        for (int step = 0; step < 5; step++) {
            //find the closest collision
            Hit collision = null;

            for (Terrain terrain : terrains) {
                if (!getTerrainCollisionMask().test(terrain.getCollisionMask()))
                    continue;

                for (Collider<?> terrainBB : terrain.getPreciseCollider()) {
                    //check for collision along the motion ray
                    Hit result = currentAABB.sweep(terrainBB, toMove);
                    if (result != null && result.tNear() >= 0f && (collision == null || result.tNear() < collision.tNear()))
                        collision = result;
                }
            }

            //no collision found - exit loop
            if (collision == null)
                break;

            //set ground state when on a floor
            if (!groundState[0] && collision.normal().y > 0)
                groundState[0] = true;

            //resolve the collision
            resolveCollision(collision, currentMotion, toMove);

            //stop if remaining movement is too small
            if (toMove.lengthSquared() < Maths.SMALL_NUMBER) {
                toMove.set(0);
                break;
            }
        }

        return toMove;
    }

    protected void resolveCollision(Hit hit, Vector3f velocity, Vector3f move) {
        Resolution.slide(hit, velocity, move);
    }

    // -- entity collisions -- //

    protected void tickEntityCollisions(AABB aabb, Vector3f toMove) {
        for (Entity entity : getWorld().getEntities(new AABB(aabb).expand(toMove))) {
            if (!(entity instanceof PhysEntity physEntity) || physEntity == this || physEntity.isRemoved() || !getEntityCollisionMask().test(physEntity.getEntityCollisionMask()))
                continue;

            Hit result = aabb.sweep(physEntity.getAABB(), toMove.lengthSquared() < Maths.SMALL_NUMBER ? SMALL_IMPULSE : toMove);
            if (result != null)
                collide(physEntity, result, toMove);
        }
    }

    protected Vector3f checkEntityCollision(PhysEntity entity, Hit result) {
        //get AABB
        AABB other = entity.getAABB();
        float force = getPushForce();

        //calculate collision
        return aabb.getOverlap(other).mul(force);
    }

    protected void collide(PhysEntity entity, Hit result, Vector3f toMove) {}

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
        this.impulse.rotate(rot);
    }

    protected float getMoveSpeed() {
        return 0.125f;
    }

    protected float getPushForce() {
        return 0.015f;
    }

    protected float getStepHeight() {
        return 0f;
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
