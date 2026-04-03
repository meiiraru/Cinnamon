package cinnamon.world.entity.projectile;

import cinnamon.math.Maths;
import cinnamon.math.collision.Hit;
import cinnamon.math.collision.Resolution;
import cinnamon.registry.EntityModelRegistry;
import cinnamon.registry.EntityRegistry;
import cinnamon.render.Camera;
import cinnamon.render.MatrixStack;
import org.joml.Vector3f;

import java.util.UUID;

public class Candy extends Projectile {

    public static final int DAMAGE = 2;
    public static final int LIFETIME = 50;
    public static final float SPEED = 1.5f;
    public static final float CRIT_CHANCE = 0.15f;
    private static final float BOUNCINESS = 0.7f;

    public Candy(UUID uuid, UUID owner) {
        super(uuid, EntityModelRegistry.CANDY.resource, DAMAGE, LIFETIME, SPEED, CRIT_CHANCE, owner);
        setGravity(0.5f);
    }

    @Override
    public void tick() {
        super.tick();
        this.rotate(0, 20, 0);
    }

    @Override
    protected void resolveCollision(Hit hit, Vector3f totalMove) {
        Resolution.bounce(hit, getMotion(), totalMove, BOUNCINESS);
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
