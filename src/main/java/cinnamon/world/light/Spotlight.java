package cinnamon.world.light;

import cinnamon.render.shader.Shader;
import org.joml.Math;

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
    public int getType() {
        return 2;
    }

    public Spotlight angle(float angle) {
        return angle(angle, angle + 5f);
    }

    public Spotlight angle(float innerAngle, float outerAngle) {
        float radiOuter = Math.toRadians(outerAngle);
        this.innerAngle = Math.cos(Math.toRadians(innerAngle));
        this.outerAngle = Math.cos(radiOuter);
        this.setFOV(radiOuter * 2f);
        return this;
    }

    public float getInnerAngle() {
        return Math.toDegrees(Math.acos(innerAngle));
    }

    public float getOuterAngle() {
        return Math.toDegrees(Math.acos(outerAngle));
    }
}
