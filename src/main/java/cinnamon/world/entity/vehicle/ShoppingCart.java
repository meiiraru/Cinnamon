package cinnamon.world.entity.vehicle;

import cinnamon.registry.EntityModelRegistry;
import cinnamon.registry.EntityRegistry;
import cinnamon.utils.Rotation;
import cinnamon.world.collisions.CollisionResolver;
import cinnamon.world.collisions.CollisionResult;
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
        vec.rotate(Rotation.X.rotationDeg(-rot.x));
        vec.rotate(Rotation.Y.rotationDeg(-rot.y));
        return vec;
    }

    @Override
    protected void collide(PhysEntity entity, CollisionResult result, Vector3f toMove) {
        if (entity instanceof ShoppingCart sc) {
            Vector3f res = new Vector3f();
            CollisionResolver.push(result, toMove, res);
            sc.setMotion(res);
        }

        super.collide(entity, result, toMove);
    }

    @Override
    public EntityRegistry getType() {
        return EntityRegistry.SHOPPING_CART;
    }
}
