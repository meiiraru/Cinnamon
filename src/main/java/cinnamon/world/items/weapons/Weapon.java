package cinnamon.world.items.weapons;

import cinnamon.utils.Resource;
import cinnamon.world.entity.Entity;
import cinnamon.world.entity.projectile.Projectile;
import cinnamon.world.items.CooldownItem;
import cinnamon.world.world.World;

import java.util.UUID;

public abstract class Weapon extends CooldownItem {

    private final int fireCooldown;
    private final int maxAmmo;

    private int firing = 0;
    private int ammo = 0;

    public Weapon(String id, Resource model, int maxAmmo, int fireCooldown, int reloadCooldown) {
        super(id, 1, 1, model, reloadCooldown);
        this.fireCooldown = fireCooldown;
        this.ammo = this.maxAmmo = maxAmmo;
    }

    @Override
    public void tick() {
        if (isOnFireCooldown())
            firing--;
        else if (isFiring())
            shoot();
        super.tick();
    }

    @Override
    public boolean fire() {
        if (isOnCooldown()) {
            if (ammo > 0 && !isOnFireCooldown()) {
                breakCooldown();
                shoot();
            }
            return super.fire();
        }

        shoot();
        return super.fire();
    }

    @Override
    public void onCooldownEnd() {
        super.onCooldownEnd();
        setAmmo(maxAmmo);
    }

    protected abstract Projectile newProjectile(UUID entity);

    protected void spawnBullet() {
        Entity source = getSource();
        World world = source.getWorld();
        Projectile projectile = newProjectile(source.getUUID());

        projectile.setPos(source.getEyePos().add(source.getLookDir().mul(0.5f)));
        projectile.setRot(source.getRot());

        world.addEntity(projectile);
    }

    protected void shoot() {
        if (ammo <= 0) {
            reload();
            return;
        }

        if (isOnFireCooldown())
            return;

        setFireCooldown();

        //spawn new bullet
        spawnBullet();

        //use ammo
        ammo--;
    }

    public void reload() {
        if (!isOnCooldown() && !isFullAmmo())
            setOnCooldown();
    }

    protected void setFireCooldown() {
        this.firing = fireCooldown;
    }

    protected void setAmmo(int count) {
        this.ammo = count;
    }

    public int getAmmo() {
        return ammo;
    }

    public int getMaxAmmo() {
        return maxAmmo;
    }

    public boolean isFullAmmo() {
        return ammo >= maxAmmo;
    }

    public boolean isOnFireCooldown() {
        return this.firing > 0;
    }

    @Override
    public void unselect() {
        super.unselect();
        breakCooldown();
    }

    @Override
    public Object getCountText() {
        return ammo;
    }
}
