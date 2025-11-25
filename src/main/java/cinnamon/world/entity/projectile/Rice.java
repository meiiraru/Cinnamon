package cinnamon.world.entity.projectile;

import cinnamon.registry.EntityModelRegistry;
import cinnamon.registry.EntityRegistry;
import cinnamon.render.Camera;
import cinnamon.render.MatrixStack;
import cinnamon.utils.Maths;
import cinnamon.world.collisions.CollisionResult;
import org.joml.Vector3f;

import java.util.UUID;

public class Rice extends Projectile {

    public static final int DAMAGE = 2;

    public Rice(UUID uuid, UUID owner, int lifetime, float speed, float critChance) {
        super(uuid, EntityModelRegistry.RICE.resource, DAMAGE, lifetime, speed, critChance, owner);
    }

    public Rice(UUID uuid, UUID owner) {
        this(uuid, owner, 1, 1, 0);
    }

    @Override
    protected void applyForces() {
        //no gravity
    }

    @Override
    protected void motionFallout() {
        //no fallout
    }

    @Override
    protected void resolveCollision(CollisionResult collision, Vector3f motion, Vector3f move) {
        remove();
    }

    @Override
    protected void applyModelPose(Camera camera, MatrixStack matrices, float delta) {
        super.applyModelPose(camera, matrices, delta);
        matrices.scale(Maths.clamp((this.lifetime - delta) / 5f, 0, 1));
    }

    @Override
    public EntityRegistry getType() {
        return EntityRegistry.RICE;
    }
}
