package mayo.world.entity.vehicle;

import mayo.registry.EntityModelRegistry;
import mayo.registry.EntityRegistry;
import mayo.world.entity.Entity;
import org.joml.Vector3f;

public class Cart extends Vehicle {

    public Cart() {
        super(EntityModelRegistry.CART.model, 1);
    }

    @Override
    public Vector3f getRiderOffset(Entity rider) {
        return new Vector3f(0, 0.4f, 0);
    }

    @Override
    protected float getMoveSpeed() {
        return 0.08f;
    }

    @Override
    protected void motionFallout() {
        this.motion.mul(0.9f, 1f, 0.9f);
    }

    @Override
    public EntityRegistry getType() {
        return EntityRegistry.CART;
    }
}
