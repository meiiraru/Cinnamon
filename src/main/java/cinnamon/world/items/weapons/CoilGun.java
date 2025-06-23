package cinnamon.world.items.weapons;

import cinnamon.registry.ItemModelRegistry;
import cinnamon.sound.SoundCategory;
import cinnamon.utils.Maths;
import cinnamon.utils.Resource;
import cinnamon.world.entity.Entity;
import cinnamon.world.entity.projectile.Candy;
import cinnamon.world.entity.projectile.Projectile;

import java.util.UUID;

public class CoilGun extends Weapon {

    private static final Resource SHOOT_SOUND = new Resource("sounds/item/weapon/coil_gun/shoot.ogg");

    public CoilGun(int maxRounds, int reloadTime, int useCooldown) {
        super(ItemModelRegistry.COIL_GUN.id, ItemModelRegistry.COIL_GUN.resource, maxRounds, reloadTime, useCooldown);
    }

    @Override
    protected Projectile newProjectile(UUID entity) {
        return new Candy(UUID.randomUUID(), entity);
    }

    @Override
    protected void spawnBullet(Entity source) {
        super.spawnBullet(source);
        if (!source.isSilent())
            source.getWorld().playSound(SHOOT_SOUND, SoundCategory.ENTITY, source.getPos()).pitch(Maths.range(0.8f, 1.2f));
    }
}
