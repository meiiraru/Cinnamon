package cinnamon.world.light;

import cinnamon.render.Camera;
import org.joml.Matrix4f;

public class DirectionalLight extends Light {

    @Override
    public void calculateLightSpaceMatrix() {
        super.calculateLightViewMatrix();
        float len = 20f, near = 1f, far = 100f;
        lightSpaceMatrix.identity().ortho(-len, len, -len, len, near, far);
        lightSpaceMatrix.mul(lightView);
    }

    @Override
    public void copyTransform(Matrix4f matrix) {
        //no transform needed for directional light
    }

    @Override
    public void calculateBounds() {
        aabb.set(getTransform().getPos()).inflate(0.5f);
    }

    @Override
    public Type getType() {
        return Type.DIRECTIONAL;
    }

    @Override
    public boolean shouldRender(Camera camera) {
        return true;
    }
}
