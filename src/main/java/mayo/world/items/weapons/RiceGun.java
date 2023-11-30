package mayo.world.items.weapons;

import mayo.model.ModelManager;
import mayo.render.Model;
import mayo.utils.Resource;
import mayo.world.entity.projectile.Projectile;
import mayo.world.entity.projectile.RiceBall;

import java.util.UUID;

public class RiceGun extends Weapon {

    private static final String ID = "Rice Gun";
    private static final Model MODEL = ModelManager.load(new Resource("models/items/rice_gun/rice_gun.obj"));

    public RiceGun(int maxRounds, int reloadTime, int useCooldown) {
        super(ID, MODEL, maxRounds, reloadTime, useCooldown);
    }

    @Override
    protected Projectile newProjectile(UUID entity) {
        return new RiceBall(UUID.randomUUID(), entity);
    }
}
