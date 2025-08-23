package cinnamon.world.light;

import cinnamon.render.shader.Shader;

public class DirectionalLight extends Light {

    public DirectionalLight() {
        super();
        castsShadows(true);
    }

    @Override
    protected void pushToShader(Shader shader, String prefix) {
        shader.setInt(prefix + "type", 3);
    }

    @Override
    public void calculateLightSpaceMatrix() {
        super.calculateLightViewMatrix();
        float len = 20f, near = 1f, far = 100f;
        lightSpaceMatrix.identity().ortho(-len, len, -len, len, near, far);
        lightSpaceMatrix.mul(lightView);
    }
}
