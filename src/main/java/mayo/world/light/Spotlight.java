package mayo.world.light;

import mayo.render.shader.Shader;
import org.joml.Vector3f;

public class Spotlight extends Light {

    private final Vector3f dir = new Vector3f(0, -1, 0);
    private float cutOff = 15f;
    private float outerCutOff = cutOff + 5f;

    @Override
    protected void pushToShader(Shader shader, String prefix) {
        super.pushToShader(shader, prefix);
        shader.setVec3(prefix + "dir", dir);
        shader.setFloat(prefix + "cutOff", (float) Math.cos(Math.toRadians(cutOff)));
        shader.setFloat(prefix + "outerCutOff", (float) Math.cos(Math.toRadians(outerCutOff)));
    }

    public Vector3f getDirection() {
        return dir;
    }

    public Spotlight direction(Vector3f direction) {
        return direction(direction.x, direction.y, direction.z);
    }

    public Spotlight direction(float x, float y, float z) {
        this.dir.set(x, y, z);
        return this;
    }

    public float getCutOff() {
        return cutOff;
    }

    public Spotlight cutOff(float cutOff) {
        return cutOff(cutOff, cutOff + 5f);
    }

    public Spotlight cutOff(float cutOff, float outerCutOff) {
        this.cutOff = cutOff;
        this.outerCutOff = outerCutOff;
        return this;
    }
}
