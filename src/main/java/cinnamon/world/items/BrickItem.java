package cinnamon.world.items;

import cinnamon.registry.ItemModelRegistry;
import cinnamon.world.entity.living.LivingEntity;
import cinnamon.world.entity.projectile.Brick;

import java.util.UUID;

public class BrickItem extends ThrowableItem {

    public static final float MIN_FORCE = 0.6f;
    public static final float MAX_FORCE = 1.5f;
    public static final int MAX_HELD_TICKS = 100;

    public BrickItem(int count) {
        super(ItemModelRegistry.BRICK.id, count, 3, ItemModelRegistry.BRICK.resource, MIN_FORCE, MAX_FORCE, MAX_HELD_TICKS);
    }

    @Override
    public Item copy() {
        return new BrickItem(getCount());
    }

    @Override
    public ItemCategory getCategory() {
        return ItemCategory.WEAPON;
    }

    @Override
    protected void spawnItem(float force) {
        LivingEntity src = getSource();
        Brick brick = new Brick(UUID.randomUUID(), src.getUUID());
        brick.setPos(src.getHandPos());
        brick.setMotion(src.getAimDir(20f).mul(force));
        src.getWorld().addEntity(brick);
    }
}
