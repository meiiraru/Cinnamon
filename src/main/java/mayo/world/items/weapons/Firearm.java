package mayo.world.items.weapons;

import mayo.world.World;
import mayo.world.entity.Entity;
import mayo.world.entity.projectile.Bullet;
import mayo.world.items.CooldownItem;

public class Firearm extends CooldownItem {

    public Firearm(String id, int maxRounds, int reloadTime) {
        super(id, maxRounds, reloadTime);
        reload();
    }

    @Override
    public void attack(Entity source) {
        super.attack(source);

        if (isOnCooldown())
            return;

        if (!shoot(source))
            this.resetCooldown();
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

        //spawn new bullet
        World world = entity.getWorld();
        Bullet bullet = new Bullet(world, entity);

        bullet.setPos(entity.getEyePos(1f));
        bullet.setRot(entity.getRot(1f));

        world.addEntity(bullet);

        //use ammo
        count--;
        return count > 0;
    }

    private void reload() {
        count = stackCount;
    }
}
