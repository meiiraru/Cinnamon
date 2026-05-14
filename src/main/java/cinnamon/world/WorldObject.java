package cinnamon.world;

import cinnamon.math.collision.AABB;
import cinnamon.render.Camera;
import cinnamon.render.MatrixStack;
import cinnamon.world.world.World;

public abstract class WorldObject {

    protected final Transform transform = new Transform();
    protected final AABB aabb = new AABB();
    protected World world;

    public void tick() {}

    public void render(Camera camera, MatrixStack matrices, float delta) {}

    public void onAdded(World world) {
        this.world = world;
    }

    public Transform getTransform() {
        return transform;
    }

    public AABB getAABB() {
        return aabb;
    }

    protected abstract void updateAABB();

    public boolean shouldRender(Camera camera) {
        return camera.isInsideFrustum(getAABB());
    }

    public boolean isAdded() {
        return world != null;
    }

    public abstract Enum<?> getType();
}
