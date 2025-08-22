package cinnamon.world.light;

import cinnamon.render.shader.Shader;
import org.joml.Vector3f;

public class DirectionalLight extends Light {

    private final Vector3f dir = new Vector3f(0, -1, 0);

    @Override
    protected void pushToShader(Shader shader, String prefix) {
        super.pushToShader(shader, prefix);
        shader.setInt(prefix + "type", 3);
        shader.setVec3(prefix + "direction", dir);
    }

    public Vector3f getDirection() {
        return dir;
    }

    public DirectionalLight direction(Vector3f direction) {
        return direction(direction.x, direction.y, direction.z);
    }

    public DirectionalLight direction(float x, float y, float z) {
        this.dir.set(x, y, z);
        return this;
    }
}
