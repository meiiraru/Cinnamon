package cinnamon.world.items.weapons;

import cinnamon.math.Maths;
import cinnamon.registry.ItemModelRegistry;
import cinnamon.sound.SoundCategory;
import cinnamon.utils.Resource;
import cinnamon.world.entity.projectile.Nail;
import cinnamon.world.entity.projectile.Projectile;
import cinnamon.world.items.Item;
import cinnamon.world.world.World;
import cinnamon.world.world.WorldClient;

import java.util.UUID;

public class NailGun extends Weapon {

    public static final Resource SHOOT_SOUND = new Resource("sounds/item/weapon/nail_gun/shoot.ogg");

    public NailGun(int maxRounds, int fireCooldown, int reloadCooldown) {
        super(ItemModelRegistry.NAIL_GUN.id, ItemModelRegistry.NAIL_GUN.resource, maxRounds, fireCooldown, reloadCooldown);
    }

    @Override
    public Item copy() {
        return new NailGun(getMaxAmmo(), getFireCooldown(), getCooldownTime());
    }

    @Override
    protected Projectile newProjectile(UUID entity) {
        return new Nail(UUID.randomUUID(), entity);
    }

    @Override
    protected Projectile spawnBullet() {
        Projectile projectile = super.spawnBullet();
        World world = getSource().getWorld();
        if (!getSource().isSilent() && world.isClientside())
            ((WorldClient) world).playSound(SHOOT_SOUND, SoundCategory.ENTITY, getSource().getTransform().getPos()).pitch(Maths.range(0.8f, 1.2f));
        return projectile;
    }
}
