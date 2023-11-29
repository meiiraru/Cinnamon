package mayo.world.items.weapons;

import mayo.model.ModelManager;
import mayo.render.Model;
import mayo.sound.SoundCategory;
import mayo.utils.Maths;
import mayo.utils.Resource;
import mayo.world.entity.Entity;
import mayo.world.entity.projectile.Candy;
import mayo.world.entity.projectile.Projectile;

public class CoilGun extends Weapon {

    private static final String ID = "Coil Gun";
    private static final Model MODEL = ModelManager.load(new Resource("models/items/coil_gun/coil_gun.obj"));
    private static final Resource SHOOT_SOUND = new Resource("sounds/pop.ogg");

    public CoilGun(int maxRounds, int reloadTime, int useCooldown) {
        super(ID, MODEL, maxRounds, reloadTime, useCooldown);
    }

    @Override
    protected Projectile newProjectile(Entity entity) {
        return new Candy(entity);
    }

    @Override
    protected void spawnBullet(Entity source) {
        super.spawnBullet(source);
        source.getWorld().playSound(SHOOT_SOUND, SoundCategory.ENTITY, source.getPos()).pitch(Maths.range(0.8f, 1.2f));
    }
}
