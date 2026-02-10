package cinnamon.world.entity.projectile;

import cinnamon.registry.EntityModelRegistry;
import cinnamon.registry.EntityRegistry;
import cinnamon.render.Camera;
import cinnamon.render.MatrixStack;
import cinnamon.utils.Maths;
import cinnamon.world.collisions.CollisionResolver;
import cinnamon.world.collisions.CollisionResult;
import org.joml.Vector3f;

import java.util.UUID;

public class Candy extends Projectile {

    public static final int DAMAGE = 2;
    public static final int LIFETIME = 50;
    public static final float SPEED = 1.5f;
    public static final float CRIT_CHANCE = 0.15f;
    private static final Vector3f BOUNCINESS = new Vector3f(0.7f, 0.7f, 0.7f);

    public Candy(UUID uuid, UUID owner) {
        super(uuid, EntityModelRegistry.CANDY.resource, DAMAGE, LIFETIME, SPEED, CRIT_CHANCE, owner);
        setGravity(0.5f);
    }

    @Override
    public void tick() {
        super.tick();
        this.rotate(0, 20);
    }

    @Override
    protected void resolveCollision(CollisionResult collision, Vector3f totalMove) {
        CollisionResolver.bounce(collision, getMotion(), totalMove, BOUNCINESS);
    }

    @Override
    protected void motionFallout() {
        //dont decrease motion
        //super.motionFallout();
    }

    @Override
    protected void applyModelPose(Camera camera, MatrixStack matrices, float delta) {
        super.applyModelPose(camera, matrices, delta);
        matrices.scale(Maths.clamp((this.lifetime - delta) / 5f, 0, 1));
    }

    @Override
    public EntityRegistry getType() {
        return EntityRegistry.CANDY;
    }
}
