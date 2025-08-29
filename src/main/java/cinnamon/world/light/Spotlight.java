package cinnamon.world.light;

import cinnamon.render.shader.Shader;

public class Spotlight extends PointLight {

    private float innerAngle, outerAngle;

    public Spotlight() {
        super();
        castsShadows(true);
        angle(15f);
    }

    @Override
    protected void pushToShader(Shader shader, String prefix) {
        super.pushToShader(shader, prefix);
        shader.setInt(prefix + "type", 2);
        shader.setFloat(prefix + "innerAngle", innerAngle);
        shader.setFloat(prefix + "outerAngle", outerAngle);
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

    public Spotlight angle(float angle) {
        return angle(angle, angle + 5f);
    }

    public Spotlight angle(float innerAngle, float outerAngle) {
        float radiOuter = (float) Math.toRadians(outerAngle);
        this.innerAngle = (float) Math.cos(Math.toRadians(innerAngle));
        this.outerAngle = (float) Math.cos(radiOuter);
        this.setFOV(radiOuter * 2f);
        return this;
    }

    public float getInnerAngle() {
        return (float) Math.toDegrees(Math.acos(innerAngle));
    }

    public float getOuterAngle() {
        return (float) Math.toDegrees(Math.acos(outerAngle));
    }
}
