package mayo.world.entity.projectile;

import mayo.model.ModelManager;
import mayo.render.Model;
import mayo.utils.Maths;
import mayo.utils.Resource;
import mayo.world.World;
import mayo.world.collisions.CollisionResult;
import mayo.world.entity.Entity;
import mayo.world.particle.SmokeParticle;
import mayo.world.particle.Particle;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;

public class RiceBall extends Projectile {

    public static final Model MODEL = ModelManager.load(new Resource("models/entities/projectile/rice_ball/rice_ball.obj"));
    public static final int DAMAGE = 15;
    public static final int LIFETIME = 2;
    public static final float SPEED = 1.25f;
    public static final float CRIT_CHANCE = 0.15f;
    public static final float SPREAD_ANGLE = 10;
    public static final int SPLIT_LIFE = 15;
    public static final int SPLIT_AMOUNT = 15;

    public RiceBall(World world, Entity owner) {
        super(MODEL, world, DAMAGE, LIFETIME, SPEED, 0f, owner);
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
            Projectile proj = new Rice(world, owner, SPLIT_LIFE, this.speed, CRIT_CHANCE);

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
}
