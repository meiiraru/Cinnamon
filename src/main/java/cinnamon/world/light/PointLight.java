package cinnamon.world.light;

import cinnamon.render.shader.Shader;
import cinnamon.render.texture.CubeMap;
import org.joml.Matrix4f;

public class PointLight extends Light {

    private float falloffStart = 3f, falloffEnd = 5f;
    private float fov = 1.5707f; //90 degrees

    private int shadowCubemapMask = 0; //based on CubeMap.Face order
    private int shadowCubemapMaskFlags = -1;

    @Override
    protected void pushToShader(Shader shader, String prefix) {
        super.pushToShader(shader, prefix);
        shader.setFloat(prefix + "falloffStart", falloffStart);
        shader.setFloat(prefix + "falloffEnd", falloffEnd);
    }

    @Override
    public void calculateLightSpaceMatrix() {
        lightSpaceMatrix.identity().perspective(fov, 1f, 0.1f, falloffEnd);
    }

    @Override
    public void copyTransform(Matrix4f matrix) {
        matrix.translate(pos).scale(falloffEnd);
    }

    @Override
    public Type getType() {
        return Type.POINT;
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

    public PointLight setShadowCubemapMask(CubeMap.Face face, Boolean enabled) {
        int offset = 1 << face.ordinal();
        if (enabled != null) {
            shadowCubemapMask = shadowCubemapMask | offset;
            shadowCubemapMaskFlags = enabled ? shadowCubemapMaskFlags | offset : shadowCubemapMaskFlags & ~offset;
        } else {
            shadowCubemapMask = shadowCubemapMask & ~offset;
        }
        return this;
    }

    public Boolean testShadowCubemapMask(CubeMap.Face face) {
        int offset = 1 << face.ordinal();
        if ((shadowCubemapMask & offset) == 0)
            return null;
        return (shadowCubemapMaskFlags & offset) != 0;
    }

    @Override
    protected void updateAABB() {
        aabb.set(pos).inflate(falloffEnd);
    }
}
