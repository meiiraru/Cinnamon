package cinnamon.world.light;

import cinnamon.render.shader.Shader;
import cinnamon.utils.Maths;
import org.joml.Math;
import org.joml.Matrix4f;

public class Spotlight extends PointLight {

    private float innerAngle, outerAngle;
    protected float cosInner, cosOuter, tanOuter;
    private float beamStrength = 1f;

    public Spotlight() {
        super();
        castsShadows(true);
        angle(15f);
    }

    @Override
    protected void pushToShader(Shader shader, String prefix) {
        super.pushToShader(shader, prefix);
        shader.setFloat(prefix + "innerAngle", cosInner);
        shader.setFloat(prefix + "outerAngle", cosOuter);
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
        float radius = height * tanOuter;

        matrix
                .translate(pos)
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
        this.cosInner = Math.cos(Math.toRadians(innerAngle));
        this.cosOuter = Math.cos(radiOuter);
        this.tanOuter = Math.tan(radiOuter);
        this.setFOV(radiOuter * 2f);

        return this;
    }

    public float getInnerAngle() {
        return innerAngle;
    }

    public float getOuterAngle() {
        return outerAngle;
    }

    public Spotlight beamStrength(float beamStrength) {
        this.beamStrength = beamStrength;
        return this;
    }

    public float getBeamStrength() {
        return beamStrength;
    }
}
