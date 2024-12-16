package cinnamon.world.entity.projectile;

import cinnamon.registry.EntityModelRegistry;
import cinnamon.registry.EntityRegistry;
import cinnamon.sound.SoundCategory;
import cinnamon.utils.Maths;
import cinnamon.utils.Resource;
import cinnamon.world.collisions.CollisionResult;
import cinnamon.world.particle.Particle;
import cinnamon.world.particle.SmokeParticle;
import org.joml.Matrix3f;
import org.joml.Vector3f;

import java.util.UUID;

public class RiceBall extends Projectile {

    private static final Resource EXPLODE_SOUND = new Resource("sounds/item.shotgun/explode.ogg");

    public static final int DAMAGE = 15;
    public static final int LIFETIME = 6;
    public static final float SPEED = 1.25f;
    public static final float CRIT_CHANCE = 0.15f;
    public static final float SPREAD_ANGLE = 10;
    public static final int SPLIT_LIFE = 15;
    public static final int SPLIT_AMOUNT = 15;

    public RiceBall(UUID uuid, UUID owner) {
        super(uuid, EntityModelRegistry.RICE_BALL.resource, DAMAGE, LIFETIME, SPEED, 0f, owner);
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
    public void remove() {
        super.remove();

        if (lifetime > 0)
            return;

        //poof particle
        Particle particle = new SmokeParticle(10, 0xFFFFFFFF);
        particle.setPos(this.getPos());
        world.addParticle(particle);

        world.playSound(EXPLODE_SOUND, SoundCategory.ENTITY, getPos());

        //get rot
        Vector3f mot = new Vector3f(motion);
        if (mot.lengthSquared() > 0f)
            mot.normalize();

        Matrix3f dir = Maths.getDirMat(motion);

        for (int i = 0; i < SPLIT_AMOUNT; i++) {
            Projectile proj = new Rice(UUID.randomUUID(), owner, SPLIT_LIFE, this.speed, CRIT_CHANCE);

            //pos
            proj.setPos(this.getPos());
            proj.setRot(Maths.dirToRot(Maths.spread(dir, SPREAD_ANGLE, SPREAD_ANGLE)));

            //add
            world.addEntity(proj);
        }
    }

    @Override
    public EntityRegistry getType() {
        return EntityRegistry.RICE_BALL;
    }
}
