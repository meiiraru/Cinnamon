package cinnamon.world.items;

import cinnamon.registry.ItemModelRegistry;
import cinnamon.world.entity.living.LivingEntity;
import cinnamon.world.entity.projectile.Potato;

import java.util.UUID;

public class PotatoItem extends Item {

    public PotatoItem(int count) {
        super(ItemModelRegistry.POTATO.id, count, 99, ItemModelRegistry.POTATO.resource);
    }

    @Override
    public Item copy() {
        return new PotatoItem(getCount());
    }

    @Override
    public boolean fire() {
        if (!super.fire())
            return false;

        setCount(getCount() - 1);
        LivingEntity src = getSource();
        Potato potato = new Potato(UUID.randomUUID(), src.getUUID());
        potato.setPos(src.getHandPos(false, 1f));
        potato.setMotion(src.getHandDir(false, 1f));
        src.getWorld().addEntity(potato);
        return true;
    }
}
