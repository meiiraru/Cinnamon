package cinnamon.world.entity.projectile;

import cinnamon.math.collision.Hit;
import cinnamon.registry.EntityModelRegistry;
import cinnamon.registry.EntityRegistry;
import cinnamon.world.entity.PhysEntity;
import org.joml.Math;
import org.joml.Vector3f;

import java.util.UUID;

public class Brick extends Projectile {

    public Brick(UUID uuid, UUID owner) {
        super(uuid, EntityModelRegistry.BRICK.resource, 8, -1, 1.25f, 0f, owner);
        setGravity(1f);
        setRot((float) (Math.random() * 360f), (float) (Math.random() * 360f), (float) (Math.random() * 360f));
    }

    @Override
    public void tick() {
        super.tick();
        this.rotate(10, 10, 5);
    }

    @Override
    protected void resolveCollision(Hit hit, Vector3f velocity, Vector3f move) {
        remove();
    }

    @Override
    protected boolean canHit(PhysEntity entity, Hit result, Vector3f toMove) {
        boolean sup = super.canHit(entity, result, toMove);
        if (!sup) return false;

        //crit if the hit position is above the entity eye height
        if (result.position().y >= entity.getTransform().getPos().y + entity.getEyeHeight())
            this.crit = true;

        return true;
    }

    @Override
    public void remove() {
        super.remove();
        //spawn particles - play sound
    }

    @Override
    public EntityRegistry getType() {
        return EntityRegistry.BRICK;
    }
}
