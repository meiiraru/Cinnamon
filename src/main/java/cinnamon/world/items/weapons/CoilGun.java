package cinnamon.world.items.weapons;

import cinnamon.registry.ItemModelRegistry;
import cinnamon.sound.SoundCategory;
import cinnamon.utils.Maths;
import cinnamon.utils.Resource;
import cinnamon.world.entity.projectile.Candy;
import cinnamon.world.entity.projectile.Projectile;
import cinnamon.world.items.Item;
import cinnamon.world.world.World;
import cinnamon.world.world.WorldClient;

import java.util.UUID;

public class CoilGun extends Weapon {

    private static final Resource SHOOT_SOUND = new Resource("sounds/item/weapon/coil_gun/shoot.ogg");

    public CoilGun(int maxRounds, int fireCooldown, int reloadCooldown) {
        super(ItemModelRegistry.COIL_GUN.id, ItemModelRegistry.COIL_GUN.resource, maxRounds, fireCooldown, reloadCooldown);
    }

    @Override
    public Item copy() {
        return new CoilGun(getMaxAmmo(), getFireCooldown(), getCooldownTime());
    }

    @Override
    protected Projectile newProjectile(UUID entity) {
        return new Candy(UUID.randomUUID(), entity);
    }

    @Override
    protected void spawnBullet() {
        super.spawnBullet();
        World world = getSource().getWorld();
        if (!getSource().isSilent() && world.isClientside())
            ((WorldClient) world).playSound(SHOOT_SOUND, SoundCategory.ENTITY, getSource().getPos()).pitch(Maths.range(0.8f, 1.2f));
    }
}
