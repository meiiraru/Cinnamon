package mayo.world.items.weapons;

import mayo.render.Model;
import mayo.world.World;
import mayo.world.entity.Entity;
import mayo.world.entity.projectile.Projectile;
import mayo.world.items.CooldownItem;

public abstract class Weapon extends CooldownItem {

    public Weapon(String id, Model model, int maxRounds, int reloadTime, int useCooldown) {
        super(id, maxRounds, model, reloadTime, useCooldown);
        reload();
    }

    @Override
    public void attack(Entity source) {
        super.attack(source);

        if (isOnCooldown() || (count > 0 && !canUse()))
            return;

        if (!shoot(source))
            this.setOnCooldown();
    }

    @Override
    public void onCooldownEnd() {
        super.onCooldownEnd();
        reload();
    }

    protected abstract Projectile newProjectile(World world, Entity entity);

    protected void spawnBullet(Entity source) {
        World world = source.getWorld();
        Projectile projectile = newProjectile(world, source);

        projectile.setPos(source.getEyePos().add(source.getLookDir().mul(0.5f)));
        projectile.setRot(source.getRot());

        world.addEntity(projectile);
    }

    private boolean shoot(Entity source) {
        if (count <= 0)
            return false;

        setUseCooldown();

        //spawn new bullet
        spawnBullet(source);

        //use ammo
        count--;
        return count > 0;
    }

    private void reload() {
        count = stackCount;
    }
}
