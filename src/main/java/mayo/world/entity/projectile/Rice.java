package mayo.world.entity.projectile;

import mayo.model.ModelManager;
import mayo.render.MatrixStack;
import mayo.render.Model;
import mayo.utils.Maths;
import mayo.utils.Resource;
import mayo.world.World;
import mayo.world.collisions.CollisionResult;
import mayo.world.entity.Entity;
import org.joml.Vector3f;

public class Rice extends Projectile {

    public static final Model MODEL = ModelManager.load(new Resource("models/entities/projectile/rice/rice.obj"));
    public static final int DAMAGE = 2;

    public Rice(World world, Entity owner, int lifetime, float speed, float critChance) {
        super(MODEL, world, DAMAGE, lifetime, speed, critChance, owner);
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
    protected void applyModelPose(MatrixStack matrices, float delta) {
        super.applyModelPose(matrices, delta);
        matrices.scale(Maths.clamp((this.lifetime - delta) / 5f, 0, 1));
    }
}
