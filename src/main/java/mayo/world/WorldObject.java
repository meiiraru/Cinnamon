package mayo.world;

import org.joml.Vector3f;

public abstract class WorldObject {

    protected final Vector3f pos = new Vector3f();
    protected World world;

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

    public abstract Enum<?> getType();
}
