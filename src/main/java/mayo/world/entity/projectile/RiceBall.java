package mayo.world.entity.projectile;

import mayo.model.ModelManager;
import mayo.render.Model;
import mayo.utils.Meth;
import mayo.utils.Resource;
import mayo.world.World;
import mayo.world.entity.Entity;
import mayo.world.particle.CloudParticle;
import mayo.world.particle.Particle;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;

public class RiceBall extends Projectile {

    public static final Model MODEL = ModelManager.load(new Resource("models/entities/projectile/rice_ball/rice_ball.obj"));
    public static final int DAMAGE = 12;
    public static final int LIFETIME = 2;
    public static final float SPEED = 1.25f;
    public static final float CRIT_CHANCE = 0.15f;
    public static final float SPREAD_ANGLE = 20;
    public static final int SPLIT_LIFE = 15;
    public static final int SPLIT_AMOUNT = 10;

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
    protected void resolveCollision(boolean x, boolean y, boolean z) {
        if (x || y || z)
            remove();
    }

    @Override
    public void remove() {
        super.remove();

        if (lifetime > 0)
            return;

        //poof particle
        Particle particle = new CloudParticle(10, -1);
        particle.setPos(this.getPos());
        world.addParticle(particle);

        Vector2f rot = this.getRot();
        Quaternionf rotation = new Quaternionf().rotationYXZ((float) Math.toRadians(-rot.y), (float) Math.toRadians(-rot.x), 0f);
        Vector3f forwards = new Vector3f(0f, 0f, -1f).rotate(rotation);
        Vector3f up = new Vector3f(0f, 1f, 0f).rotate(rotation);
        Vector3f left = new Vector3f(-1f, 0f, 0f).rotate(rotation);

        for (int i = 0; i < SPLIT_AMOUNT; i++) {
            Projectile proj = new Rice(world, owner, SPLIT_LIFE, this.speed, CRIT_CHANCE);

            //pos
            proj.setPos(this.getPos());

            //rot
            float randomX = (float) Math.toRadians(Math.random() * 2 - 1);
            float randomY = (float) Math.toRadians(Math.random() * 2 - 1);
            float randomPitch = randomX * SPREAD_ANGLE;
            float randomYaw = randomY * SPREAD_ANGLE;

            float transformX = (float)(Math.cos(randomPitch) * Math.cos(randomYaw));
            float transformY = (float)Math.sin(randomYaw);
            float transformZ = (float)(Math.sin(randomPitch) * Math.cos(randomYaw));

            Vector3f f = forwards.mul(transformX, new Vector3f());
            Vector3f u = up.mul(transformY, new Vector3f());
            Vector3f l = left.mul(transformZ, new Vector3f());
            Vector3f finalDir = new Vector3f().add(f).add(u).add(l).normalize();

            proj.setRot(Meth.dirToRot(finalDir));

            //add
            world.addEntity(proj);
        }
    }
}
