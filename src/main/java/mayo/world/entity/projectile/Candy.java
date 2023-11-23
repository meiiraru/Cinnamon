package mayo.world.entity.projectile;

import mayo.model.ModelManager;
import mayo.render.MatrixStack;
import mayo.render.Model;
import mayo.utils.Maths;
import mayo.utils.Resource;
import mayo.world.World;
import mayo.world.collisions.CollisionResolver;
import mayo.world.collisions.CollisionResult;
import mayo.world.entity.Entity;
import org.joml.Vector3f;

public class Candy extends Projectile {

    public static final Model MODEL = ModelManager.load(new Resource("models/entities/projectile/candy/candy.obj"));
    public static final int DAMAGE = 2;
    public static final int LIFETIME = 50;
    public static final float SPEED = 1.5f;
    public static final float CRIT_CHANCE = 0.15f;
    private static final Vector3f BOUNCINESS = new Vector3f(0.7f, 0.7f, 0.7f);

    public Candy(World world, Entity owner) {
        super(MODEL, world, DAMAGE, LIFETIME, SPEED, CRIT_CHANCE, owner);
    }

    @Override
    public void tick() {
        super.tick();
        this.rotate(0, 20);
    }

    @Override
    protected void applyForces() {
        //less gravity
        this.motion.y -= world.gravity * 0.5f;
    }

    @Override
    protected void resolveCollision(CollisionResult collision, Vector3f motion, Vector3f move) {
        CollisionResolver.bounce(collision, motion, move, BOUNCINESS);
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
