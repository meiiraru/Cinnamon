package mayo.world.entity;

import mayo.render.Model;
import mayo.utils.AABB;
import mayo.world.World;
import mayo.world.collisions.CollisionDetector;
import mayo.world.collisions.CollisionResolver;
import mayo.world.collisions.CollisionResult;
import org.joml.Vector3f;

import java.util.List;

public abstract class PhysEntity extends Entity {

    protected final Vector3f
            motion = new Vector3f(),
            move = new Vector3f();

    protected boolean onGround;

    public PhysEntity(Model model, World world) {
        super(model, world);
    }

    public void tick() {
        super.tick();

        //apply ambient forces
        applyForces();

        //apply my current desired movement into my motion
        applyMovement();
        move.set(0);
        onGround = false;

        //check for terrain collisions
        Vector3f toMove = tickCollisions();

        //move entity
        moveTo(pos.x + toMove.x, pos.y + toMove.y, pos.z + toMove.z);

        //decrease motion
        motionFallout();

        //entity collisions
        for (Entity entity : world.getEntities(aabb))
            if (entity != this && !entity.isRemoved())
                collide(entity);
    }

    protected void applyMovement() {
        this.motion.add(move.mul(onGround ? 1 : 0.125f));
    }

    protected void applyForces() {
        //gravity
        this.motion.y -= world.gravity;
    }

    protected Vector3f tickCollisions() {
        //early exit
        if (motion.lengthSquared() < 0.001f)
            return motion.mul(0);

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
        float l = Math.signum(left);
        float u = Math.signum(up);
        float f = Math.signum(forwards);

        this.move.set(l, u, -f);
        if (move.lengthSquared() > 1)
            move.normalize();

        move.mul(getMoveSpeed());

        //move the entity in facing direction
        this.move.rotateX((float) Math.toRadians(-rot.x));
        this.move.rotateY((float) Math.toRadians(-rot.y));
    }

    protected float getMoveSpeed() {
        return 0.15f;
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

    protected float getPushForce() {
        return 0.015f;
    }

    protected void collide(Entity entity) {}

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
        this.motion.add(dir.x * force, dir.y * force, dir.z * force);
    }
}
