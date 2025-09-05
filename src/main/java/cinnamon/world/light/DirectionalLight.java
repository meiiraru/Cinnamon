package cinnamon.world.light;

import cinnamon.render.Camera;

public class DirectionalLight extends Light {

    public DirectionalLight() {
        super();
        castsShadows(true);
    }

    @Override
    public void calculateLightSpaceMatrix() {
        super.calculateLightViewMatrix();
        float len = 20f, near = 1f, far = 100f;
        lightSpaceMatrix.identity().ortho(-len, len, -len, len, near, far);
        lightSpaceMatrix.mul(lightView);
    }

    @Override
    protected void updateAABB() {
        aabb.set(pos).inflate(0.5f);
    }

    @Override
    public int getType() {
        return 3;
    }

    @Override
    public boolean shouldRender(Camera camera) {
        return getIntensity() > 0f;
    }
}
