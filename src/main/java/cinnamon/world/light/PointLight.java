package cinnamon.world.light;

import cinnamon.render.Camera;
import cinnamon.render.shader.Shader;

public class PointLight extends Light {

    private float falloffStart = 3f, falloffEnd = 5f;
    private float fov = 1.5707f; //90 degrees

    @Override
    protected void pushToShader(Shader shader, String prefix) {
        super.pushToShader(shader, prefix);
        shader.setFloat(prefix + "falloffStart", falloffStart);
        shader.setFloat(prefix + "falloffEnd", falloffEnd);
    }

    @Override
    public void calculateLightSpaceMatrix() {
        lightSpaceMatrix.identity().perspective(fov, 1f, 0.5f, falloffEnd);
    }

    @Override
    public int getType() {
        return 1;
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

    protected void setFOV(float fov) {
        this.fov = fov;
    }

    public float getFOV() {
        return fov;
    }

    @Override
    protected void updateAABB() {
        aabb.set(pos).inflate(falloffEnd);
    }

    @Override
    public boolean shouldRenderFlare(Camera camera) {
        return getFlareIntensity() > 0f;
    }
}
