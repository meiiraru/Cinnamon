package cinnamon.world.entity.collectable;

import cinnamon.Client;
import cinnamon.math.collision.AABB;
import cinnamon.math.collision.Hit;
import cinnamon.math.collision.Resolution;
import cinnamon.render.Camera;
import cinnamon.render.DebugRenderer;
import cinnamon.render.MatrixStack;
import cinnamon.utils.Resource;
import cinnamon.world.entity.PhysEntity;
import org.joml.Math;
import org.joml.Vector3f;

import java.util.UUID;

public abstract class Collectable extends PhysEntity {

    private static final float BOUNCINESS = 0.5f;

    protected final AABB entityAABB = new AABB();

    public Collectable(UUID uuid, Resource model) {
        super(uuid, model);
    }

    @Override
    public void tick() {
        super.tick();
        this.rotate(0, 1, 0);
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
        DebugRenderer.renderAABB(matrices, entityAABB, 0xFF00FF00);
    }

    @Override
    protected void tickEntityCollisions(AABB aabb, Vector3f toMove) {
        super.tickEntityCollisions(entityAABB, toMove);
    }

    @Override
    protected void collide(PhysEntity entity, Hit result, Vector3f toMove) {
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
    protected void resolveCollision(Hit hit, Vector3f totalMove) {
        Resolution.bounce(hit, getMotion(), totalMove, BOUNCINESS);
    }

    @Override
    public void rotateTo(float pitch, float yaw, float roll) {
        super.rotateTo(0, yaw, 0);
    }

    protected abstract boolean onPickUp(PhysEntity entity);
}
