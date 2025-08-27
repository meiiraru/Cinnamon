package cinnamon.world.light;

import cinnamon.render.shader.Shader;

public class PointLight extends Light {

    private float falloffStart = 3f, falloffEnd = 5f;
    protected float fov = (float) Math.toRadians(90f);

    @Override
    protected void pushToShader(Shader shader, String prefix) {
        shader.setInt(prefix + "type", 1);
        shader.setFloat(prefix + "falloffStart", falloffStart);
        shader.setFloat(prefix + "falloffEnd", falloffEnd);
    }

    @Override
    public void calculateLightSpaceMatrix() {
        lightSpaceMatrix.identity().perspective(fov, 1f, 0.5f, falloffEnd);
    }

    @Override
    public boolean isDirectional() {
        return false;
    }

    public PointLight falloff(float falloff) {
        return falloff(falloff, falloff + 5f);
    }

    public PointLight falloff(float falloffStart, float falloffEnd) {
        this.falloffStart = falloffStart;
        this.falloffEnd = falloffEnd;
        updateAABB();
        return this;
    }

    public float getFalloffStart() {
        return falloffStart;
    }

    public float getFalloffEnd() {
        return falloffEnd;
    }

    public float getFOV() {
        return fov;
    }

    @Override
    protected void updateAABB() {
        aabb.set(pos).inflate(falloffEnd);
    }
}
