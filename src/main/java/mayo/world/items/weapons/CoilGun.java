package mayo.world.items.weapons;

import mayo.model.ModelManager;
import mayo.render.Model;
import mayo.utils.Resource;
import mayo.world.World;
import mayo.world.entity.Entity;
import mayo.world.entity.projectile.Candy;
import mayo.world.entity.projectile.Projectile;

public class CoilGun extends Firearm {

    private static final String ID = "Coil Gun";
    private static final Model MODEL = ModelManager.load(new Resource("models/items/coil_gun/coil_gun.obj"));

    public CoilGun(int maxRounds, int reloadTime, int useCooldown) {
        super(ID, MODEL, maxRounds, reloadTime, useCooldown);
    }

    @Override
    protected Projectile newProjectile(World world, Entity entity) {
        return new Candy(world, entity);
    }
}