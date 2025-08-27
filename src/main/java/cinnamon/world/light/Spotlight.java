package cinnamon.world.light;

import cinnamon.render.shader.Shader;

public class Spotlight extends PointLight {

    private float innerCutOff = 0.9659f, outerCutOff = 0.9397f; // cos(15) and cos(20)

    public Spotlight() {
        super();
        castsShadows(true);
    }

    @Override
    protected void pushToShader(Shader shader, String prefix) {
        super.pushToShader(shader, prefix);
        shader.setInt(prefix + "type", 2);
        shader.setFloat(prefix + "innerCutOff", innerCutOff);
        shader.setFloat(prefix + "outerCutOff", outerCutOff);
    }

    @Override
    public void calculateLightSpaceMatrix() {
        super.calculateLightViewMatrix();
        super.calculateLightSpaceMatrix();
        lightSpaceMatrix.mul(lightView);
    }

    @Override
    public boolean isDirectional() {
        return true;
    }

    public Spotlight cutOff(float cutOff) {
        return cutOff(cutOff, cutOff + 5f);
    }

    public Spotlight cutOff(float innerCutOff, float outerCutOff) {
        float radiOuter = (float) Math.toRadians(outerCutOff);
        this.innerCutOff = (float) Math.cos(Math.toRadians(innerCutOff));
        this.outerCutOff = (float) Math.cos(radiOuter);
        this.fov = radiOuter * 2f;
        return this;
    }
}
