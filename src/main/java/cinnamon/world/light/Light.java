package cinnamon.world.light;

import cinnamon.render.shader.Shader;
import org.joml.Vector3f;

public class Light {

    private final Vector3f pos = new Vector3f();
    private int color = 0xFFFFFF;
    private float intensity = 5f, falloffStart = 3f, falloffEnd = 5f;

    public void pushToShader(Shader shader, int index) {
        String prefix = "lights[" + index + "].";
        pushToShader(shader, prefix);
    }

    protected void pushToShader(Shader shader, String prefix) {
        shader.setVec3(prefix + "pos", pos);
        shader.setColor(prefix + "color", color);
        shader.setInt(prefix + "type", 1);

        shader.setFloat(prefix + "intensity", intensity);
        shader.setFloat(prefix + "falloffStart", falloffStart);
        shader.setFloat(prefix + "falloffEnd", falloffEnd);
    }

    public Vector3f getPos() {
        return pos;
    }

    public Light pos(Vector3f pos) {
        return pos(pos.x, pos.y, pos.z);
    }

    public Light pos(float x, float y, float z) {
        this.pos.set(x, y, z);
        return this;
    }

    public Light color(int color) {
        this.color = color;
        return this;
    }

    public int getColor() {
        return color;
    }

    public Light intensity(float intensity) {
        this.intensity = intensity;
        return this;
    }

    public float getIntensity() {
        return intensity;
    }

    public Light falloff(float falloff) {
        return falloff(falloff, falloff + 5f);
    }

    public Light falloff(float falloffStart, float falloffEnd) {
        this.falloffStart = falloffStart;
        this.falloffEnd = falloffEnd;
        return this;
    }

    public float getFalloffStart() {
        return falloffStart;
    }

    public float getFalloffEnd() {
        return falloffEnd;
    }
}
