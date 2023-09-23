package mayo.world.entity.projectile;

import mayo.model.ModelManager;
import mayo.render.Model;
import mayo.utils.Resource;
import mayo.world.World;
import mayo.world.entity.Entity;

public class Bullet extends Projectile {

    private static final Model MODEL = ModelManager.load(new Resource("models/entities/bullet/bullet.obj"));
    private static final int DAMAGE = 3;
    private static final int LIFETIME = 30;
    private static final float SPEED = 0.5f;
    private static final float CRIT_CHANCE = 0.15f;

    public Bullet(World world, Entity owner) {
        super(MODEL, world, MODEL.getMesh().getBoundingBox(), DAMAGE, LIFETIME, SPEED, Math.random() < CRIT_CHANCE);
        this.setOwner(owner);
    }
}
