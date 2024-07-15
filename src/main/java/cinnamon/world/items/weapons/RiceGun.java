package cinnamon.world.items.weapons;

import cinnamon.model.ModelManager;
import cinnamon.render.Model;
import cinnamon.utils.Resource;
import cinnamon.world.entity.projectile.Projectile;
import cinnamon.world.entity.projectile.RiceBall;

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
