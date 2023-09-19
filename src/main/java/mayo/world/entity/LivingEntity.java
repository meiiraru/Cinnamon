package mayo.world.entity;

import mayo.world.items.Item;
import mayo.utils.AABB;
import mayo.world.World;

public abstract class LivingEntity extends Entity {

    private Item holdingItem;

    public LivingEntity(World world, AABB boundingBox) {
        super(world, boundingBox);
    }

    public void setHoldingItem(Item holdingItem) {
        this.holdingItem = holdingItem;
    }

    public Item getHoldingItem() {
        return holdingItem;
    }
}
