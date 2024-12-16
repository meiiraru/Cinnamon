package cinnamon.world.items.weapons;

import cinnamon.registry.ItemModelRegistry;
import cinnamon.sound.SoundCategory;
import cinnamon.utils.Resource;
import cinnamon.world.entity.Entity;
import cinnamon.world.entity.projectile.Projectile;
import cinnamon.world.entity.projectile.RiceBall;

import java.util.UUID;

public class RiceGun extends Weapon {

    private static final Resource SHOOT_SOUND = new Resource("sounds/item.shotgun/shoot.ogg");

    public RiceGun(int maxRounds, int reloadTime, int useCooldown) {
        super(ItemModelRegistry.RICE_GUN.id, ItemModelRegistry.RICE_GUN.resource, maxRounds, reloadTime, useCooldown);
    }

    @Override
    protected Projectile newProjectile(UUID entity) {
        return new RiceBall(UUID.randomUUID(), entity);
    }

    @Override
    protected void spawnBullet(Entity source) {
        super.spawnBullet(source);
        source.getWorld().playSound(SHOOT_SOUND, SoundCategory.ENTITY, source.getPos());
    }
}
