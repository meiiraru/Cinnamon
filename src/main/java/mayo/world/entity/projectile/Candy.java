package mayo.world.entity.projectile;

import mayo.model.ModelManager;
import mayo.render.Model;
import mayo.utils.Resource;
import mayo.world.World;
import mayo.world.entity.Entity;

public class Candy extends Projectile {

    public static final Model MODEL = ModelManager.load(new Resource("models/entities/candy/candy.obj"));
    public static final int DAMAGE = 2;
    public static final int LIFETIME = 30;
    public static final float SPEED = 1.75f;
    public static final float CRIT_CHANCE = 0.15f;

    public Candy(World world, Entity owner) {
        super(MODEL, world, DAMAGE, LIFETIME, SPEED, CRIT_CHANCE, owner);
    }

    @Override
    public void tick() {
        super.tick();
        this.rotate(0, rot.y + 20);
    }
}
