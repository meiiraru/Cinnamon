package mayo.world.entity.projectile;

import mayo.model.ModelManager;
import mayo.render.Model;
import mayo.utils.Resource;
import mayo.world.World;
import mayo.world.entity.Entity;
import mayo.world.particle.CloudParticle;
import mayo.world.particle.Particle;
import org.joml.Vector2f;

public class RiceBall extends Projectile {

    public static final Model MODEL = ModelManager.load(new Resource("models/entities/projectile/rice_ball/rice_ball.obj"));
    public static final int DAMAGE = 12;
    public static final int LIFETIME = 2;
    public static final float SPEED = 1.25f;
    public static final float CRIT_CHANCE = 0.15f;
    public static final float SPREAD_ANGLE = 20;
    public static final int SPLIT_LIFE = 15;
    public static final int SPLIT_AMOUNT = 1000;

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

        float half = SPREAD_ANGLE * 0.5f;
        for (int i = 0; i < SPLIT_AMOUNT; i++) {
            Projectile proj = new Rice(world, owner, SPLIT_LIFE, this.speed, CRIT_CHANCE);

            //pos
            proj.setPos(this.getPos());

            //rot
            Vector2f rot = this.getRot();
            proj.setRot(
                    rot.x + (float) Math.random() * SPREAD_ANGLE - half,
                    rot.y + (float) Math.random() * SPREAD_ANGLE - half
            );

            //add
            world.addEntity(proj);
        }
    }
}
