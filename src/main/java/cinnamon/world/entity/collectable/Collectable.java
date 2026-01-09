package cinnamon.world.entity.collectable;

import cinnamon.Client;
import cinnamon.model.GeometryHelper;
import cinnamon.render.Camera;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.utils.AABB;
import cinnamon.utils.Resource;
import cinnamon.world.collisions.CollisionResolver;
import cinnamon.world.collisions.CollisionResult;
import cinnamon.world.entity.Entity;
import cinnamon.world.entity.PhysEntity;
import org.joml.Math;
import org.joml.Vector3f;

import java.util.UUID;

public abstract class Collectable extends PhysEntity {

    private static final Vector3f BOUNCINESS = new Vector3f(0.5f, 0.5f, 0.5f);

    protected final AABB entityAABB = new AABB();

    public Collectable(UUID uuid, Resource model) {
        super(uuid, model);
    }

    @Override
    public void tick() {
        super.tick();
        this.rotate(0, 1);
    }

    @Override
    protected void applyModelPose(Camera camera, MatrixStack matrices, float delta) {
        matrices.translate(0, Math.sin((Client.getInstance().ticks + delta) * 0.05f) * getAABB().getHeight() * 0.075f, 0);
        super.applyModelPose(camera, matrices, delta);
    }

    @Override
    public void renderDebugHitbox(MatrixStack matrices, float delta) {
        super.renderDebugHitbox(matrices, delta);
        //entity bounding box
        Vector3f min = entityAABB.getMin(), max = entityAABB.getMax();
        VertexConsumer.LINES.consume(GeometryHelper.box(matrices, min.x, min.y, min.z, max.x, max.y, max.z, 0xFF00FF00));
    }

    @Override
    protected void tickEntityCollisions(AABB aabb, Vector3f toMove) {
        super.tickEntityCollisions(entityAABB, toMove);
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
        this.aabb.inflate(0.25f);
        if (entityAABB != null)
            this.entityAABB.set(this.aabb);
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
