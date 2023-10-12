package mayo.world.entity.collectable;

import mayo.Client;
import mayo.render.MatrixStack;
import mayo.render.Model;
import mayo.utils.AABB;
import mayo.world.World;
import mayo.world.entity.Entity;
import mayo.world.entity.PhysEntity;

import java.util.List;

public abstract class Collectable extends PhysEntity {

    public Collectable(Model model, World world) {
        super(model, world);
    }

    @Override
    public void tick() {
        super.tick();
        this.rotate(0, Client.getInstance().ticks);
    }

    @Override
    protected void applyModelPose(MatrixStack matrices, float delta) {
        matrices.translate(0, ((float) Math.sin((Client.getInstance().ticks + delta) * 0.05f) + 1) * 0.15f, 0);
        super.applyModelPose(matrices, delta);
    }

    @Override
    protected void collide(Entity entity) {
        super.collide(entity);
        if (!isRemoved() && onPickUp(entity))
            this.remove();
    }

    @Override
    protected void updateAABB() {
        super.updateAABB();
        this.aabb.inflate(0.25f, 0f, 0.25f, 0.25f, 0.5f, 0.25f);
    }

    @Override
    protected void resolveCollision(List<AABB.CollisionResult> collisions) {
        //bounce
        //if (x) this.motion.x *= -0.5f;
        //if (y) this.motion.y *= -0.5f;
        //if (z) this.motion.z *= -0.5f;
    }

    protected abstract boolean onPickUp(Entity entity);
}
