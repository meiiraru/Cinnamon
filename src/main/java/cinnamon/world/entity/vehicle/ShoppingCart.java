package cinnamon.world.entity.vehicle;

import cinnamon.math.collision.Hit;
import cinnamon.math.collision.Resolution;
import cinnamon.registry.EntityModelRegistry;
import cinnamon.registry.EntityRegistry;
import cinnamon.world.entity.Entity;
import cinnamon.world.entity.PhysEntity;
import org.joml.Vector3f;

import java.util.UUID;

public class ShoppingCart extends Car {

    public ShoppingCart(UUID uuid) {
        super(uuid, EntityModelRegistry.SHOPPING_CART.resource, 1, 20f, 0.0002f, 0.003f, 0.003f, 0.9f);
    }

    @Override
    public Vector3f getRiderOffset(Entity rider) {
        Vector3f vec = new Vector3f(0, 0.5f, 0);
        vec.rotate(rot);
        return vec;
    }

    @Override
    protected void collide(PhysEntity entity, Hit result, Vector3f toMove) {
        if (entity instanceof ShoppingCart) {
            if (this.getAABB().intersectsAABB(entity.getAABB()))
                return;

            if (!this.getRiders().isEmpty() && entity.getRiders().isEmpty()) {
                Vector3f res = new Vector3f();
                Resolution.push(result, toMove, res);
                entity.setMotion(res);
                return;
            } else if (this.getRiders().isEmpty() && !entity.getRiders().isEmpty()) {
                return;
            }
        }

        super.collide(entity, result, toMove);
    }

    @Override
    public EntityRegistry getType() {
        return EntityRegistry.SHOPPING_CART;
    }
}
