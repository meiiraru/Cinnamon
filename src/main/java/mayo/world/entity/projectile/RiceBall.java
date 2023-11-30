package mayo.world.entity.projectile;

import mayo.registry.EntityModelRegistry;
import mayo.registry.EntityRegistry;
import mayo.utils.Maths;
import mayo.world.collisions.CollisionResult;
import mayo.world.particle.Particle;
import mayo.world.particle.SmokeParticle;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.UUID;

public class RiceBall extends Projectile {

    public static final int DAMAGE = 15;
    public static final int LIFETIME = 2;
    public static final float SPEED = 1.25f;
    public static final float CRIT_CHANCE = 0.15f;
    public static final float SPREAD_ANGLE = 10;
    public static final int SPLIT_LIFE = 15;
    public static final int SPLIT_AMOUNT = 15;

    public RiceBall(UUID uuid, UUID owner) {
        super(uuid, EntityModelRegistry.RICE_BALL.model, DAMAGE, LIFETIME, SPEED, 0f, owner);
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
        Particle particle = new SmokeParticle(10, -1);
        particle.setPos(this.getPos());
        world.addParticle(particle);

        //get rot
        Vector3f vec = new Vector3f(motion);
        if (vec.lengthSquared() > 0f)
            vec.normalize();

        Vector2f rot = Maths.toRadians(Maths.dirToRot(vec));
        Quaternionf rotation = new Quaternionf().rotationYXZ(-rot.y, -rot.x, 0f);

        Vector3f left = new Vector3f(1f, 0f, 0f).rotate(rotation);
        Vector3f up = new Vector3f(0f, 1f, 0f).rotate(rotation);
        Vector3f forwards = new Vector3f(0f, 0f, -1f).rotate(rotation);

        for (int i = 0; i < SPLIT_AMOUNT; i++) {
            Projectile proj = new Rice(UUID.randomUUID(), owner, SPLIT_LIFE, this.speed, CRIT_CHANCE);

            //pos
            proj.setPos(this.getPos());

            //random rot
            float r1 = (float) Math.toRadians(Math.random() * 2 - 1) * SPREAD_ANGLE;
            float r2 = (float) Math.toRadians(Math.random() * 2 - 1) * SPREAD_ANGLE;

            float transformL = (float) (Math.sin(r1) * Math.cos(r2));
            float transformU = (float) Math.sin(r2);
            float transformF = (float) (Math.cos(r1) * Math.cos(r2));

            proj.setRot(Maths.dirToRot(
                    left.x * transformL + up.x * transformU + forwards.x * transformF,
                    left.y * transformL + up.y * transformU + forwards.y * transformF,
                    left.z * transformL + up.z * transformU + forwards.z * transformF
            ));

            //add
            world.addEntity(proj);
        }
    }

    @Override
    public EntityRegistry getType() {
        return EntityRegistry.RICE_BALL;
    }
}
