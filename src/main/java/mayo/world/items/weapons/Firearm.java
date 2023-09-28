package mayo.world.items.weapons;

import mayo.world.World;
import mayo.world.entity.Entity;
import mayo.world.entity.projectile.Bullet;
import mayo.world.items.CooldownItem;

public class Firearm extends CooldownItem {

    public Firearm(String id, int maxRounds, int reloadTime, int useCooldown) {
        super(id, maxRounds, reloadTime, useCooldown);
        reload();
    }

    @Override
    public void attack(Entity source) {
        super.attack(source);

        if (isOnCooldown())
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

    private boolean shoot(Entity entity) {
        if (count <= 0)
            return false;

        if (!canUse())
            return true;

        setUseCooldown();

        //spawn new bullet
        World world = entity.getWorld();
        Bullet bullet = new Bullet(world, entity);

        bullet.setPos(entity.getEyePos().sub(0, bullet.getDimensions().y / 2f, 0));
        bullet.setRot(entity.getRot());

        world.addEntity(bullet);

        //use ammo
        count--;
        return count > 0;
    }

    private void reload() {
        count = stackCount;
    }
}
