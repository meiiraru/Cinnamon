package mayo.world.entity.projectile;

import mayo.model.ModelManager;
import mayo.render.Model;
import mayo.utils.Resource;
import mayo.world.World;
import mayo.world.entity.Entity;

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
    protected void applyForces() {
        super.applyForces();
        acceleration.add(0, world.gravity, 0);
    }
}
