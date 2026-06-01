package cinnamon.world.items;

import cinnamon.registry.ItemModelRegistry;
import cinnamon.world.entity.living.LivingEntity;
import cinnamon.world.entity.projectile.Brick;

import java.util.UUID;

public class BrickItem extends Item {

    public BrickItem(int count) {
        super(ItemModelRegistry.BRICK.id, count, 3, ItemModelRegistry.BRICK.resource);
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
    public boolean fire() {
        if (!super.fire())
            return false;

        setCount(getCount() - 1);
        LivingEntity src = getSource();
        Brick brick = new Brick(UUID.randomUUID(), src.getUUID());
        brick.setPos(src.getHandPos());
        brick.setMotion(src.getAimDir(20f));
        src.getWorld().addEntity(brick);
        return true;
    }
}
