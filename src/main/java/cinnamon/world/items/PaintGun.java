package cinnamon.world.items;

import cinnamon.registry.ItemModelRegistry;
import cinnamon.sound.SoundCategory;
import cinnamon.utils.Colors;
import cinnamon.utils.Maths;
import cinnamon.utils.Resource;
import cinnamon.world.entity.living.LivingEntity;
import cinnamon.world.entity.projectile.PaintBall;
import cinnamon.world.world.World;
import cinnamon.world.world.WorldClient;

import java.util.UUID;

public class PaintGun extends CooldownItem {

    public static final Resource SHOOT_SOUND = new Resource("sounds/item/weapon/paint_gun/shoot.ogg");

    public PaintGun() {
        super(ItemModelRegistry.PAINT_GUN.id, 1, 1, ItemModelRegistry.PAINT_GUN.resource, 3);
    }

    @Override
    public Item copy() {
        return new PaintGun();
    }

    @Override
    public void tick() {
        if (isFiring())
            shoot();
        super.tick();
    }

    @Override
    public boolean fire() {
        if (!super.fire())
            return false;
        shoot();
        return true;
    }

    private void shoot() {
        LivingEntity source = getSource();
        World world = source.getWorld();
        if (world == null || !world.isClientside() || this.isOnCooldown())
            return;

        PaintBall projectile = new PaintBall(UUID.randomUUID(), source.getUUID(), Colors.randomRainbow().argb);

        projectile.setPos(source.getHandPos(false, 1f));
        projectile.setRot(Maths.dirToRot(source.getHandDir(false, 1f)));
        projectile.impulse(0, 0, 1);

        world.addEntity(projectile);

        if (!source.isSilent())
            ((WorldClient) world).playSound(SHOOT_SOUND, SoundCategory.ENTITY, source.getPos()).pitch(Maths.range(0.8f, 1f));

        this.setOnCooldown();
    }

    public Object getCountText() {
        return "";
    }
}
