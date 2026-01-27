package cinnamon.world.items;

import cinnamon.registry.ItemModelRegistry;
import cinnamon.sound.SoundCategory;
import cinnamon.utils.Colors;
import cinnamon.utils.Maths;
import cinnamon.utils.Resource;
import cinnamon.world.entity.living.LivingEntity;
import cinnamon.world.entity.projectile.PaintBall;
import cinnamon.world.entity.projectile.Projectile;
import cinnamon.world.items.weapons.Weapon;
import cinnamon.world.world.World;
import cinnamon.world.world.WorldClient;

import java.util.UUID;

public class PaintGun extends Weapon {

    private static final Resource SHOOT_SOUND = new Resource("sounds/item/weapon/paint_gun/shoot.ogg");
    private static final Resource RELOAD_START_SOUND = new Resource("sounds/item/weapon/paint_gun/reload_start.ogg");
    private static final Resource RELOAD_END_SOUND = new Resource("sounds/item/weapon/paint_gun/reload_end.ogg");
    private static final Resource HAMMER_SOUND = new Resource("sounds/item/weapon/paint_gun/hammer.ogg");

    private static final int MAX_AMMO = 10;
    private static final int FIRE_COOLDOWN = 3;
    private static final int RELOAD_COOLDOWN = 40;
    private static final int HAMMER_TIME = 10;

    private boolean hammerEngaged = false;

    public PaintGun() {
        super(ItemModelRegistry.PAINT_GUN.id, ItemModelRegistry.PAINT_GUN.resource, MAX_AMMO, FIRE_COOLDOWN, RELOAD_COOLDOWN);
    }

    @Override
    public Item copy() {
        return new PaintGun();
    }

    @Override
    protected Projectile newProjectile(UUID entity) {
        return new PaintBall(UUID.randomUUID(), entity, Colors.randomRainbow().argb);
    }

    @Override
    protected void spawnBullet() {
        super.spawnBullet();
        LivingEntity source = getSource();
        World world = source.getWorld();
        if (!source.isSilent() && world.isClientside())
            ((WorldClient) world).playSound(SHOOT_SOUND, SoundCategory.ENTITY, source.getPos()).pitch(Maths.range(0.8f, 1f));
    }

    @Override
    public void reload() {
        if (!isOnCooldown() && !isFullAmmo()) {
            playReloadStartSound();
            hammerEngaged = false;
        }
        super.reload();
    }

    @Override
    public void tick() {
        super.tick();
        if (isOnCooldown() && !hammerEngaged && getCooldown() <= HAMMER_TIME) {
            hammerEngaged = true;
            playHammerSound();
        }
    }

    @Override
    public void onCooldownEnd() {
        super.onCooldownEnd();
        playReloadEndSound();
    }

    private void playReloadStartSound() {
        LivingEntity source = getSource();
        World world = source.getWorld();
        if (world != null && world.isClientside() && !source.isSilent())
            ((WorldClient) world).playSound(RELOAD_START_SOUND, SoundCategory.ENTITY, source.getPos());
    }

    private void playReloadEndSound() {
        LivingEntity source = getSource();
        World world = source.getWorld();
        if (world != null && world.isClientside() && !source.isSilent())
            ((WorldClient) world).playSound(RELOAD_END_SOUND, SoundCategory.ENTITY, source.getPos());
    }

    private void playHammerSound() {
        LivingEntity source = getSource();
        World world = source.getWorld();
        if (world != null && world.isClientside() && !source.isSilent())
            ((WorldClient) world).playSound(HAMMER_SOUND, SoundCategory.ENTITY, source.getPos());
    }
}
