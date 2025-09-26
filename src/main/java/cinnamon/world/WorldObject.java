package cinnamon.world;

import cinnamon.render.Camera;
import cinnamon.render.MatrixStack;
import cinnamon.utils.AABB;
import cinnamon.world.world.World;
import org.joml.Vector3f;

public abstract class WorldObject {

    protected final Vector3f pos = new Vector3f();
    protected final AABB aabb = new AABB();
    protected World world;

    public void tick() {}

    public void render(Camera camera, MatrixStack matrices, float delta) {}

    public void onAdded(World world) {
        this.world = world;
    }

    public void setPos(Vector3f pos) {
        this.setPos(pos.x, pos.y, pos.z);
    }

    public void setPos(float x, float y, float z) {
        this.pos.set(x, y, z);
    }

    public Vector3f getPos() {
        return pos;
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
