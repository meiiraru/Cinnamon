package mayo.world.entity;

import mayo.utils.AABB;
import mayo.world.World;

public abstract class Entity {

    private final AABB boundingBox;
    private World world;

    public Entity(World world, AABB boundingBox) {
        this.world = world;
        this.boundingBox = boundingBox;
    }

    public AABB getBoundingBox() {
        return boundingBox;
    }
}
