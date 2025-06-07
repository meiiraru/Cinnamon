package cinnamon.world.entity.collectable;

import cinnamon.Client;
import cinnamon.render.MatrixStack;
import cinnamon.utils.Resource;
import cinnamon.world.collisions.CollisionResolver;
import cinnamon.world.collisions.CollisionResult;
import cinnamon.world.entity.Entity;
import cinnamon.world.entity.PhysEntity;
import org.joml.Vector3f;

import java.util.UUID;

public abstract class Collectable extends PhysEntity {

    private static final Vector3f BOUNCINESS = new Vector3f(0.5f, 0.5f, 0.5f);

    public Collectable(UUID uuid, Resource model) {
        super(uuid, model);
    }

    @Override
    public void tick() {
        super.tick();
        this.rotate(0, 1);
    }

    @Override
    protected void applyModelPose(MatrixStack matrices, float delta) {
        matrices.translate(0, ((float) Math.sin((Client.getInstance().ticks + delta) * 0.05f) + 1) * 0.15f, 0);
        super.applyModelPose(matrices, delta);
    }

    @Override
    protected void collide(Entity entity, CollisionResult result, Vector3f toMove) {
        super.collide(entity, result, toMove);
        if (!isRemoved() && onPickUp(entity))
            this.remove();
    }

    @Override
    protected void updateAABB() {
        super.updateAABB();
        this.aabb.inflate(0.25f, 0f, 0.25f, 0.25f, 0.5f, 0.25f);
    }

    @Override
    protected void resolveCollision(CollisionResult collision, Vector3f motion, Vector3f move) {
        CollisionResolver.bounce(collision, motion, move, BOUNCINESS);
    }

    @Override
    public void rotateTo(float pitch, float yaw) {
        super.rotateTo(0, yaw);
    }

    protected abstract boolean onPickUp(Entity entity);
}
