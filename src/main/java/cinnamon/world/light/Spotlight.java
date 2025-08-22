package cinnamon.world.light;

import cinnamon.render.shader.Shader;
import org.joml.Vector3f;

public class Spotlight extends Light {

    private final Vector3f dir = new Vector3f(0f, -1f, 0f);
    private float innerCutOff = 0.9659f, outerCutOff = 0.9397f; // cos(15) and cos(20)

    @Override
    protected void pushToShader(Shader shader, String prefix) {
        super.pushToShader(shader, prefix);
        shader.setInt(prefix + "type", 2);
        shader.setVec3(prefix + "direction", dir);
        shader.setFloat(prefix + "innerCutOff", innerCutOff);
        shader.setFloat(prefix + "outerCutOff", outerCutOff);
    }

    public Vector3f getDirection() {
        return dir;
    }

    public Spotlight direction(Vector3f direction) {
        return direction(direction.x, direction.y, direction.z);
    }

    public Spotlight direction(float x, float y, float z) {
        this.dir.set(x, y, z).normalize();
        return this;
    }

    public Spotlight cutOff(float cutOff) {
        return cutOff(cutOff, cutOff + 5f);
    }

    public Spotlight cutOff(float innerCutOff, float outerCutOff) {
        this.innerCutOff = (float) Math.cos(Math.toRadians(innerCutOff));
        this.outerCutOff = (float) Math.cos(Math.toRadians(outerCutOff));
        return this;
    }
}
