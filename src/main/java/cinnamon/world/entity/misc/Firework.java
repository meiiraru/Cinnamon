package cinnamon.world.entity.misc;

import cinnamon.registry.EntityModelRegistry;
import cinnamon.registry.EntityRegistry;
import cinnamon.sound.SoundCategory;
import cinnamon.utils.AABB;
import cinnamon.utils.Maths;
import cinnamon.utils.Resource;
import cinnamon.world.DamageType;
import cinnamon.world.collisions.CollisionResult;
import cinnamon.world.entity.Entity;
import cinnamon.world.entity.PhysEntity;
import cinnamon.world.entity.living.LivingEntity;
import cinnamon.world.particle.FireParticle;
import cinnamon.world.particle.SmokeParticle;
import cinnamon.world.world.World;
import cinnamon.world.world.WorldClient;
import org.joml.Math;
import org.joml.Vector3f;

import java.util.UUID;

public class Firework extends PhysEntity {

    public static final Resource
        EXPLOSION_SOUND = new Resource("sounds/entity/misc/firework/explosion.ogg"),
        LAUNCH_SOUND = new Resource("sounds/entity/misc/firework/launch.ogg");

    protected final FireworkStar[] stars;
    protected int life;

    public Firework(UUID uuid, int lifetime, Vector3f velocity, FireworkStar... stars) {
        super(uuid, EntityModelRegistry.FIREWORK.resource);
        this.life = lifetime;
        this.stars = stars;
        setMotion(velocity);
        setGravity(0f);
    }

    @Override
    public void onAdded(World world) {
        super.onAdded(world);
        if (!isSilent() && getWorld().isClientside())
            ((WorldClient) getWorld()).playSound(LAUNCH_SOUND, SoundCategory.ENTITY, getPos()).pitch(Maths.range(0.8f, 1.2f)).volume(0.3f);
    }

    @Override
    public void tick() {
        super.tick();

        life--;
        if (life <= 0)
            explode();

        if (getWorld().isClientside())
            flyParticles();

        Vector3f vec = new Vector3f(motion);
        if (vec.lengthSquared() > 0f)
            vec.normalize();

        this.rotateTo(Maths.dirToRot(vec));
    }

    @Override
    protected void applyImpulse() {
        this.motion.add(impulse);
        this.impulse.set(0);
    }

    @Override
    protected void motionFallout() {
        //super.motionFallout();
    }

    @Override
    protected void resolveCollision(CollisionResult collision, Vector3f totalMove) {
        explode();
    }

    @Override
    protected void collide(PhysEntity entity, CollisionResult result, Vector3f toMove) {
        explode();
    }

    @Override
    public EntityRegistry getType() {
        return EntityRegistry.FIREWORK;
    }

    protected void flyParticles() {
        WorldClient wc = (WorldClient) getWorld();
        Vector3f pos = getPos();

        FireParticle fire = new FireParticle(5);
        fire.setPos(pos);
        wc.addParticle(fire);

        for (int i = 0; i < 3; i++) {
            SmokeParticle smoke = new SmokeParticle(10, 0xFF888888);
            smoke.setPos(pos);
            Vector3f dir = Maths.spread(new Vector3f(0, -1, 0), 45, 45);
            smoke.setMotion(dir.mul((float) Math.random() * 0.05f + 0.05f));
            wc.addParticle(smoke);
        }
    }

    protected void explode() {
        if (stars.length > 0) {
            World w = getWorld();
            Vector3f pos = getPos();

            AABB explosionBB = new AABB().inflate(2f).translate(pos);
            for (Entity entity : w.getEntities(explosionBB)) {
                if (entity instanceof LivingEntity living && !living.isRemoved())
                    living.damage(this, DamageType.EXPLOSION, 2 * stars.length, false);
            }

            if (!isSilent() && w.isClientside())
                ((WorldClient) w).playSound(EXPLOSION_SOUND, SoundCategory.ENTITY, pos).pitch(Maths.range(0.8f, 1.2f)).volume(0.3f).distance(96).maxDistance(160);

            for (FireworkStar star : stars)
                star.explode(this);
        }

        remove();
    }
}
