package cinnamon.world.entity.projectile;

import cinnamon.math.Maths;
import cinnamon.math.collision.AABB;
import cinnamon.math.collision.Collider;
import cinnamon.math.collision.Hit;
import cinnamon.math.collision.Resolution;
import cinnamon.registry.EntityModelRegistry;
import cinnamon.registry.EntityRegistry;
import cinnamon.render.Camera;
import cinnamon.render.MatrixStack;
import cinnamon.world.terrain.Terrain;
import org.joml.Math;
import org.joml.Vector3f;

import java.util.UUID;

public class Nail extends Projectile {

    public static final int LIFETIME = 600;

    protected boolean collided = false;

    public Nail(UUID uuid, UUID owner) {
        super(uuid, EntityModelRegistry.NAIL.resource, 1, LIFETIME, 3f, 0.1f, owner);
        this.setGravity(1f);
        this.setCanSelfDamage(true);
    }

    @Override
    public void tick() {
        super.tick();

        //rotate to motion direction
        if (!collided) {
            Vector3f vec = new Vector3f(motion);
            if (vec.lengthSquared() > 0f) {
                vec.normalize();
                this.rotateTo(Maths.dirToQuat(vec).rotateY(Math.PI_f));
            }
        }
    }

    @Override
    protected void tickPhysics() {
        if (collided) {
            AABB bb = getAABB();
            for (Terrain terrain : getWorld().getTerrains(getAABB())) {
                for (Collider<?> collider : terrain.getPreciseCollider()) {
                    if (collider.intersectsAABB(bb))
                        return;
                }
            }

            this.setGravity(1f);
            this.collided = false;
            this.lifetime = LIFETIME;
            return;
        }

        super.tickPhysics();
    }

    @Override
    protected void collideTerrain(Terrain terrain, Hit hit, Vector3f velocity, Vector3f move) {
        Resolution.stick(hit, velocity, move);

        Vector3f halfSize = getAABB().getDimensions().mul(0.5f);
        Vector3f offset = getLookDir().mul(halfSize, halfSize);
        move.add(offset);

        collided = true;
        setGravity(0f);
    }

    @Override
    protected void applyModelPose(Camera camera, MatrixStack matrices, float delta) {
        super.applyModelPose(camera, matrices, delta);
        matrices.scale(Maths.clamp((this.lifetime - delta) / 5f, 0, 1));
    }

    @Override
    public EntityRegistry getType() {
        return EntityRegistry.NAIL;
    }
}
