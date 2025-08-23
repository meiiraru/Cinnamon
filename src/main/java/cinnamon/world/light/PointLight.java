package cinnamon.world.light;

import cinnamon.render.shader.Shader;

public class PointLight extends Light {

    private float falloffStart = 3f, falloffEnd = 5f;

    @Override
    protected void pushToShader(Shader shader, String prefix) {
        shader.setInt(prefix + "type", 1);
        shader.setFloat(prefix + "falloffStart", falloffStart);
        shader.setFloat(prefix + "falloffEnd", falloffEnd);
    }

    public PointLight falloff(float falloff) {
        return falloff(falloff, falloff + 5f);
    }

    public PointLight falloff(float falloffStart, float falloffEnd) {
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
