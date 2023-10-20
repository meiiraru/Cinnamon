package mayo.world.items.weapons;

import mayo.model.ModelManager;
import mayo.render.Model;
import mayo.sound.SoundCategory;
import mayo.utils.Maths;
import mayo.utils.Resource;
import mayo.world.World;
import mayo.world.entity.Entity;
import mayo.world.entity.projectile.Potato;
import mayo.world.entity.projectile.Projectile;

public class PotatoCannon extends Weapon {

    private static final String ID = "Potato Cannon";
    private static final Model MODEL = ModelManager.load(new Resource("models/items/potato_cannon/potato_cannon.obj"));
    private static final Resource SHOOT_SOUND = new Resource("sounds/pvc.ogg");

    public PotatoCannon(int maxRounds, int reloadTime, int useCooldown) {
        super(ID, MODEL, maxRounds, reloadTime, useCooldown);
    }

    @Override
    protected Projectile newProjectile(World world, Entity entity) {
        return new Potato(world, entity);
    }

    @Override
    protected void spawnBullet(Entity source) {
        super.spawnBullet(source);
        source.getWorld().playSound(SHOOT_SOUND, SoundCategory.ENTITY, source.getPos()).pitch(Maths.range(0.5f, 0.8f));
    }
}
