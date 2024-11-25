package cinnamon.world.items.weapons;

import cinnamon.registry.ItemModelRegistry;
import cinnamon.world.entity.projectile.Projectile;
import cinnamon.world.entity.projectile.RiceBall;

import java.util.UUID;

public class RiceGun extends Weapon {

    public RiceGun(int maxRounds, int reloadTime, int useCooldown) {
        super(ItemModelRegistry.RICE_GUN.id, ItemModelRegistry.RICE_GUN.resource, maxRounds, reloadTime, useCooldown);
    }

    @Override
    protected Projectile newProjectile(UUID entity) {
        return new RiceBall(UUID.randomUUID(), entity);
    }
}
