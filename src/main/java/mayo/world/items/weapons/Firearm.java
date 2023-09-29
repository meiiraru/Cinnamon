package mayo.world.items.weapons;

import mayo.render.Model;
import mayo.world.World;
import mayo.world.entity.Entity;
import mayo.world.entity.projectile.Projectile;
import mayo.world.items.CooldownItem;

public abstract class Firearm extends CooldownItem {

    public Firearm(String id, Model model, int maxRounds, int reloadTime, int useCooldown) {
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
    public boolean hasAttack() {
        return true;
    }

    @Override
    public void onCooldownEnd() {
        super.onCooldownEnd();
        reload();
    }

    protected abstract Projectile newProjectile(World world, Entity entity);

    private boolean shoot(Entity entity) {
        if (count <= 0)
            return false;

        setUseCooldown();

        //spawn new bullet
        World world = entity.getWorld();
        Projectile projectile = newProjectile(world, entity);

        projectile.setPos(entity.getEyePos().add(entity.getLookDir().mul(0.5f)));
        projectile.setRot(entity.getRot());

        world.addEntity(projectile);

        //use ammo
        count--;
        return count > 0;
    }

    private void reload() {
        count = stackCount;
    }
}
