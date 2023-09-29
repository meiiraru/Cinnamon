package mayo.world.entity.projectile;

import mayo.model.ModelManager;
import mayo.render.Model;
import mayo.utils.Meth;
import mayo.utils.Resource;
import mayo.world.World;
import mayo.world.entity.Entity;
import org.joml.Vector3f;

public class Potato extends Projectile {

    public static final Model MODEL = ModelManager.load(new Resource("models/entities/potato/potato.obj"));
    public static final int DAMAGE = 8;
    public static final int LIFETIME = 100;
    public static final float SPEED = 0.75f;
    public static final float CRIT_CHANCE = 0.15f;

    public Potato(World world, Entity owner) {
        super(MODEL, world, DAMAGE, LIFETIME, SPEED, CRIT_CHANCE, owner);
    }

    @Override
    public void tick() {
        super.tick();

        Vector3f vec = new Vector3f(velocity);
        if (vec.lengthSquared() > 0f)
            vec.normalize();

        this.rotate(Meth.dirToRot(vec));
    }

    @Override
    protected void applyForces() {
        super.applyForces();
        acceleration.add(0, world.gravity, 0);
    }
}
