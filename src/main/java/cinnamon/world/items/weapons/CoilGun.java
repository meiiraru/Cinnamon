package cinnamon.world.items.weapons;

import cinnamon.model.ModelManager;
import cinnamon.render.Model;
import cinnamon.sound.SoundCategory;
import cinnamon.utils.Maths;
import cinnamon.utils.Resource;
import cinnamon.world.entity.Entity;
import cinnamon.world.entity.projectile.Candy;
import cinnamon.world.entity.projectile.Projectile;

import java.util.UUID;

public class CoilGun extends Weapon {

    private static final String ID = "Coil Gun";
    private static final Model MODEL = ModelManager.load(new Resource("models/items/coil_gun/coil_gun.obj"));
    private static final Resource SHOOT_SOUND = new Resource("sounds/pop.ogg");

    public CoilGun(int maxRounds, int reloadTime, int useCooldown) {
        super(ID, MODEL, maxRounds, reloadTime, useCooldown);
    }

    @Override
    protected Projectile newProjectile(UUID entity) {
        return new Candy(UUID.randomUUID(), entity);
    }

    @Override
    protected void spawnBullet(Entity source) {
        super.spawnBullet(source);
        source.getWorld().playSound(SHOOT_SOUND, SoundCategory.ENTITY, source.getPos()).pitch(Maths.range(0.8f, 1.2f));
    }
}
