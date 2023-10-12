package mayo.world.entity;

import mayo.render.Model;
import mayo.utils.AABB;
import mayo.world.World;
import org.joml.Vector3f;

import java.util.ArrayList;
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

        //apply forces like gravity:tm:
        applyForces();

        //apply my current desired movement into my motion
        applyMovement();

        //move the entity using the motion
        List<AABB.CollisionResult> collisionResult = checkCollisions(motion);

        //collision
        if (!collisionResult.isEmpty()) {
            resolveCollision(collisionResult);
        } else {
            onGround = false;
        }

        moveTo(pos.x + motion.x, pos.y + motion.y, pos.z + motion.z);

        //decrease motion
        motionFallout();

        //clear input
        move.set(0);

        //collide entities
        for (Entity entity : world.getEntities(aabb))
            if (entity != this && !entity.isRemoved())
                collide(entity);
    }

    protected void applyMovement() {
        this.motion.add(move.mul(onGround ? 1 : 0.125f));
    }

    protected void applyForces() {
        //that's gravity and stuff:tm:
        this.motion.y -= world.gravity;
    }

    protected void resolveCollision(List<AABB.CollisionResult> collisions) {
        for (AABB.CollisionResult collision : collisions) {
            Vector3f n = collision.normal();
            if (n.y > 0)
                this.onGround = true;

            this.motion.set(
                    n.x != 0 ? 0 : motion.x,
                    n.y != 0 ? 0 : motion.y,
                    n.z != 0 ? 0 : motion.z
            );
        }
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
        this.move.set(left, up, -forwards);
        this.move.rotateX((float) Math.toRadians(-rot.x));
        this.move.rotateY((float) Math.toRadians(-rot.y));
    }

    protected float getMoveSpeed() {
        return 0.15f;
    }

    protected List<AABB.CollisionResult> checkCollisions(Vector3f vec) {
        List<AABB.CollisionResult> collisions = new ArrayList<>();

        Vector3f pos = aabb.getCenter();
        Vector3f inflation = aabb.getDimensions().mul(0.5f);

        //get terrain collisions
        AABB area = new AABB(aabb).expand(vec);
        List<AABB> terrain = world.getTerrainCollisions(area);

        for (AABB terrainBB : terrain) {
            AABB.CollisionResult result = new AABB(terrainBB).inflate(inflation).collisionRay(pos, vec);
            if (result != null)
                collisions.add(result);
        }

        collisions.sort((o1, o2) -> Float.compare(o1.delta(), o2.delta()));
        return collisions;
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
