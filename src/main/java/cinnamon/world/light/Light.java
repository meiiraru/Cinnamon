package cinnamon.world.light;

import cinnamon.model.GeometryHelper;
import cinnamon.model.Vertex;
import cinnamon.render.Camera;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.render.shader.Shader;
import cinnamon.settings.Settings;
import cinnamon.utils.AABB;
import cinnamon.utils.Resource;
import cinnamon.utils.Rotation;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public abstract class Light {

    public static final Resource
        LAMP = new Resource("textures/environment/lamp.png"),
        LAMP_OVERLAY = new Resource("textures/environment/lamp_overlay.png");

    protected final Vector3f
            pos = new Vector3f(),
            dir = new Vector3f(0f, -1f, 0f);
    private int color = 0xFFFFFF;
    private float intensity = 5f;

    protected final AABB aabb = new AABB();
    protected final Matrix4f
            lightSpaceMatrix = new Matrix4f(),
            lightView = new Matrix4f();
    private boolean castsShadows = false;

    public void pushToShader(Shader shader) {
        pushToShader(shader, -1);
    }

    public void pushToShader(Shader shader, int index) {
        String prefix = index < 0 ? "light." : "lights[" + index + "].";
        shader.setVec3(prefix + "pos", pos);
        shader.setVec3(prefix + "direction", dir);
        shader.setColor(prefix + "color", color);
        shader.setFloat(prefix + "intensity", intensity);
        shader.setMat4(prefix + "lightSpaceMatrix", lightSpaceMatrix);
        shader.setBool(prefix + "castsShadows", castsShadows());
        pushToShader(shader, prefix);
    }

    protected abstract void pushToShader(Shader shader, String prefix);

    public abstract void calculateLightSpaceMatrix();

    protected abstract void updateAABB();

    public abstract boolean isDirectional();

    public boolean shouldRender(Camera camera) {
        return intensity > 0f && camera.getPos().distanceSquared(getPos()) <= 9216 && camera.isInsideFrustum(aabb); //96 * 96
    }

    protected void calculateLightViewMatrix() {
        float x = 0f, y = 1f;

        if (Math.abs(dir.dot(0f, 1f, 0f)) > 0.999f) {
            x = 1f;
            y = 0f;
        }

        lightView.identity().lookAt(
                pos.x, pos.y, pos.z,
                pos.x + dir.x, pos.y + dir.y, pos.z + dir.z,
                x, y, 0f
        );
    }

    public void renderDebug(Camera camera, MatrixStack matrices) {
        if (camera.getPos().distanceSquared(pos) <= 0.1f)
            return;

        matrices.pushMatrix();
        matrices.translate(pos);
        camera.billboard(matrices);
        matrices.rotate(Rotation.Z.rotationDeg(180f));

        Vertex[] v = GeometryHelper.quad(matrices, -0.5f, -0.5f, 1f, 1f);
        VertexConsumer.MAIN.consume(v, LAMP);

        int c = color | 0xFF000000;
        for (Vertex vertex : v)
            vertex.color(c);
        VertexConsumer.MAIN.consume(v, LAMP_OVERLAY);

        matrices.popMatrix();

        if (isDirectional())
            VertexConsumer.MAIN.consume(GeometryHelper.line(
                    matrices,
                    pos.x, pos.y, pos.z,
                    pos.x + dir.x, pos.y + dir.y, pos.z + dir.z,
                    0.025f, c
            ));
    }

    public Vector3f getPos() {
        return pos;
    }

    public Light pos(Vector3f pos) {
        return pos(pos.x, pos.y, pos.z);
    }

    public Light pos(float x, float y, float z) {
        this.pos.set(x, y, z);
        updateAABB();
        return this;
    }

    public Vector3f getDirection() {
        return dir;
    }

    public Light direction(Vector3f direction) {
        return direction(direction.x, direction.y, direction.z);
    }

    public Light direction(float x, float y, float z) {
        this.dir.set(x, y, z).normalize();
        updateAABB();
        return this;
    }

    public Light color(int color) {
        this.color = color;
        return this;
    }

    public int getColor() {
        return color;
    }

    public Light intensity(float intensity) {
        this.intensity = intensity;
        return this;
    }

    public float getIntensity() {
        return intensity;
    }

    public AABB getAABB() {
        return aabb;
    }

    public Matrix4f getLightSpaceMatrix() {
        return lightSpaceMatrix;
    }

    public boolean castsShadows() {
        return castsShadows && Settings.shadowQuality.get() != -1;
    }

    public Light castsShadows(boolean bool) {
        this.castsShadows = bool;
        return this;
    }
}
