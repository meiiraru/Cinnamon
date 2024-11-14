package cinnamon.world.items.weapons;

import cinnamon.model.ModelManager;
import cinnamon.render.model.ModelRenderer;
import cinnamon.sound.SoundCategory;
import cinnamon.utils.Maths;
import cinnamon.utils.Resource;
import cinnamon.world.entity.Entity;
import cinnamon.world.entity.projectile.Potato;
import cinnamon.world.entity.projectile.Projectile;

import java.util.UUID;

public class PotatoCannon extends Weapon {

    private static final String ID = "Potato Cannon";
    private static final ModelRenderer MODEL = ModelManager.load(new Resource("models/items/potato_cannon/potato_cannon.obj"));
    private static final Resource SHOOT_SOUND = new Resource("sounds/pvc.ogg");

    public PotatoCannon(int maxRounds, int reloadTime, int useCooldown) {
        super(ID, MODEL, maxRounds, reloadTime, useCooldown);
    }

    @Override
    protected Projectile newProjectile(UUID entity) {
        return new Potato(UUID.randomUUID(), entity);
    }

    @Override
    protected void spawnBullet(Entity source) {
        super.spawnBullet(source);
        source.getWorld().playSound(SHOOT_SOUND, SoundCategory.ENTITY, source.getPos()).pitch(Maths.range(0.5f, 0.8f));
    }
}
