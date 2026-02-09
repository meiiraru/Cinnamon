package cinnamon.world.light;

import cinnamon.model.GeometryHelper;
import cinnamon.render.Camera;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.render.shader.Shader;
import org.joml.Matrix4f;

public class PointLight extends Light {

    private float falloffStart = 3f, falloffEnd = 5f;
    private float fov = 1.5707f; //90 degrees

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
    public void renderDebug(Camera camera, MatrixStack matrices) {
        super.renderDebug(camera, matrices);
        renderDebugMesh(matrices);
    }

    protected void renderDebugMesh(MatrixStack matrices) {
        matrices.pushMatrix();
        matrices.translate(pos);
        matrices.scale(falloffEnd);
        VertexConsumer.LINES.consume(GeometryHelper.sphere(matrices, 0, 0, 0, 1f, 12, getColor() | 0xFF000000));

        matrices.popMatrix();
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

    @Override
    protected void updateAABB() {
        aabb.set(pos).inflate(falloffEnd);
    }
}
