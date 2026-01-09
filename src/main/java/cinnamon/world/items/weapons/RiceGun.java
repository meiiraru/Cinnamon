package cinnamon.world.items.weapons;

import cinnamon.registry.ItemModelRegistry;
import cinnamon.world.entity.projectile.Projectile;
import cinnamon.world.entity.projectile.RiceBall;
import cinnamon.world.items.Item;

import java.util.UUID;

public class RiceGun extends Weapon {

    public RiceGun(int maxRounds, int fireCooldown, int reloadCooldown) {
        super(ItemModelRegistry.RICE_GUN.id, ItemModelRegistry.RICE_GUN.resource, maxRounds, fireCooldown, reloadCooldown);
    }

    @Override
    public Item copy() {
        return new RiceGun(getMaxAmmo(), getFireCooldown(), getCooldownTime());
    }

    @Override
    protected Projectile newProjectile(UUID entity) {
        return new RiceBall(UUID.randomUUID(), entity);
    }
}
