package cinnamon.world.items.weapons;

import cinnamon.registry.ItemModelRegistry;
import cinnamon.sound.SoundCategory;
import cinnamon.utils.Maths;
import cinnamon.utils.Resource;
import cinnamon.world.entity.Entity;
import cinnamon.world.entity.projectile.Potato;
import cinnamon.world.entity.projectile.Projectile;

import java.util.UUID;

public class PotatoCannon extends Weapon {

    private static final Resource SHOOT_SOUND = new Resource("sounds/item/weapon/potato_cannon/shoot.ogg");

    public PotatoCannon(int maxRounds, int reloadTime, int useCooldown) {
        super(ItemModelRegistry.POTATO_CANNON.id, ItemModelRegistry.POTATO_CANNON.resource, maxRounds, reloadTime, useCooldown);
    }

    @Override
    protected Projectile newProjectile(UUID entity) {
        return new Potato(UUID.randomUUID(), entity);
    }

    @Override
    protected void spawnBullet(Entity source) {
        super.spawnBullet(source);
        source.getWorld().playSound(SHOOT_SOUND, SoundCategory.ENTITY, source.getPos()).pitch(Maths.range(0.5f, 0.8f));
    }
}
