package mayo.world.entity;

import mayo.utils.AABB;
import mayo.world.World;

public class Player extends LivingEntity {

    public Player(World world, AABB boundingBox) {
        super(world, boundingBox);
    }
}
