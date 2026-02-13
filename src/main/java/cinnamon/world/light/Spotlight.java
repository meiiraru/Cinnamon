package cinnamon.world.light;

import cinnamon.model.GeometryHelper;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.render.shader.Shader;
import cinnamon.utils.Maths;
import cinnamon.utils.Rotation;
import org.joml.Math;
import org.joml.Matrix4f;

public class Spotlight extends PointLight {

    private float innerAngle, outerAngle;
    private float tanOuter;
    private float beamStrength = 1f;

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
    protected void renderDebugMesh(MatrixStack matrices) {
        float height = getFalloffEnd();
        float radius = height * tanOuter;

        matrices.pushMatrix();

        matrices
                .translate(pos)
                .rotate(Maths.dirToQuat(dir))
                .rotate(Rotation.X.rotation(-Math.PI_OVER_2_f))
                .scale(radius, height, radius);
        VertexConsumer.LINES.consume(GeometryHelper.cone(matrices, 0, -1, 0, 1f, 1f, 12, getColor() | 0xFF000000));

        matrices.popMatrix();
    }

    @Override
    public Type getType() {
        return Type.SPOT;
    }

    public Spotlight angle(float angle) {
        return angle(angle, angle + 5f);
    }

    public Spotlight angle(float innerAngle, float outerAngle) {
        float radiOuter = Math.toRadians(outerAngle);
        this.innerAngle = Math.cos(Math.toRadians(innerAngle));
        this.outerAngle = Math.cos(radiOuter);
        this.tanOuter   = Math.tan(radiOuter);
        this.setFOV(radiOuter * 2f);
        return this;
    }

    public float getInnerAngle() {
        return Math.toDegrees(Math.acos(innerAngle));
    }

    public float getOuterAngle() {
        return Math.toDegrees(Math.acos(outerAngle));
    }

    public Spotlight beamStrength(float beamStrength) {
        this.beamStrength = beamStrength;
        return this;
    }

    public float getBeamStrength() {
        return beamStrength;
    }
}
