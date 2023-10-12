package mayo.world.entity.projectile;

import mayo.model.ModelManager;
import mayo.render.MatrixStack;
import mayo.render.Model;
import mayo.utils.AABB;
import mayo.utils.Maths;
import mayo.utils.Resource;
import mayo.world.World;
import mayo.world.entity.Entity;

import java.util.List;

public class Candy extends Projectile {

    public static final Model MODEL = ModelManager.load(new Resource("models/entities/projectile/candy/candy.obj"));
    public static final int DAMAGE = 2;
    public static final int LIFETIME = 50;
    public static final float SPEED = 1.5f;
    public static final float CRIT_CHANCE = 0.15f;

    public Candy(World world, Entity owner) {
        super(MODEL, world, DAMAGE, LIFETIME, SPEED, CRIT_CHANCE, owner);
    }

    @Override
    public void tick() {
        super.tick();
        this.rotate(0, rot.y + 20);
    }

    @Override
    protected void applyForces() {
        //less gravity
        this.motion.y -= world.gravity * 0.5f;
    }

    @Override
    protected void resolveCollision(List<AABB.CollisionResult> collisions) {
        //bounce
        //if (x) this.motion.x *= -0.7f;
        //if (y) this.motion.y *= -0.7f;
        //if (z) this.motion.z *= -0.7f;
    }

    @Override
    protected void motionFallout() {
        //dont decrease motion
        //super.motionFallout();
    }

    @Override
    protected void applyModelPose(MatrixStack matrices, float delta) {
        super.applyModelPose(matrices, delta);
        matrices.scale(Maths.clamp((this.lifetime - delta) / 5f, 0, 1));
    }
}
