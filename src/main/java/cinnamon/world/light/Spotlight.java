package cinnamon.world.light;

import cinnamon.math.Maths;
import cinnamon.render.shader.Shader;
import org.joml.Math;
import org.joml.Matrix4f;

public class Spotlight extends PointLight {

    private float innerAngle, outerAngle;
    protected float innerCutoff, outerCutoff, coneRadiusMult;

    public Spotlight() {
        super();
        angle(15f);
        volumetricStrength(1f);
    }

    @Override
    protected void pushToShader(Shader shader, String prefix) {
        super.pushToShader(shader, prefix);
        shader.setFloat(prefix + "innerAngle", innerCutoff);
        shader.setFloat(prefix + "outerAngle", outerCutoff);
    }

    @Override
    public void calculateLightSpaceMatrix() {
        super.calculateLightViewMatrix();
        super.calculateLightSpaceMatrix();
        lightSpaceMatrix.mul(lightView);
    }

    @Override
    public void copyTransform(Matrix4f matrix) {
        float height = getFalloffEnd();
        float radius = height * coneRadiusMult;

        matrix
                .translate(getTransform().getPos())
                .rotate(Maths.dirToQuat(dir))
                .rotateX(-Math.PI_OVER_2_f)
                .scale(radius, height, radius);
    }

    @Override
    public Type getType() {
        return Type.SPOT;
    }

    public Spotlight angle(float angle) {
        return angle(angle, angle + 5f);
    }

    public Spotlight angle(float innerAngle, float outerAngle) {
        this.innerAngle = innerAngle;
        this.outerAngle = outerAngle;

        float radiOuter = Math.toRadians(outerAngle);
        this.innerCutoff = Math.cos(Math.toRadians(innerAngle));
        this.outerCutoff = Math.cos(radiOuter);
        this.coneRadiusMult = Math.tan(radiOuter);
        this.setFOV(radiOuter * 2f);

        return this;
    }

    public float getInnerAngle() {
        return innerAngle;
    }

    public float getOuterAngle() {
        return outerAngle;
    }

    public float getInnerCutoff() {
        return innerCutoff;
    }

    public float getOuterCutoff() {
        return outerCutoff;
    }

    public float getConeRadiusMultiplier() {
        return coneRadiusMult;
    }
}
