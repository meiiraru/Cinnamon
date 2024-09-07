package cinnamon.world.entity.vehicle;

import cinnamon.registry.EntityModelRegistry;
import cinnamon.registry.EntityRegistry;
import cinnamon.world.entity.Entity;
import org.joml.Vector3f;

import java.util.UUID;

public class Cart extends Vehicle {

    private boolean isRailed;


    public Cart(UUID uuid) {
        super(uuid, EntityModelRegistry.CART.model, 1);
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
    public void rotateTo(float pitch, float yaw) {
        super.rotateTo(isRailed ? pitch : 0, yaw);
    }

    public void setRailed(boolean railed) {
        isRailed = railed;
    }

    @Override
    public EntityRegistry getType() {
        return EntityRegistry.CART;
    }
}
