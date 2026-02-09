package cinnamon.world.light;

import cinnamon.gui.DebugScreen;
import cinnamon.model.GeometryHelper;
import cinnamon.model.Vertex;
import cinnamon.render.Camera;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.render.shader.Shader;
import cinnamon.utils.AABB;
import cinnamon.utils.Resource;
import cinnamon.utils.Rotation;
import cinnamon.world.Mask;
import org.joml.Math;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.UUID;

public abstract class Light {

    public static final Resource
        LAMP = new Resource("textures/environment/light/lamp.png"),
        LAMP_OVERLAY = new Resource("textures/environment/light/lamp_overlay.png"),
        GLARE = new Resource("textures/environment/light/glare.png");

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
    protected final Mask shadowMask = new Mask(0b1, 0b10);
    protected UUID source;

    private Resource glareTexture = GLARE;
    private float glareIntensity = 1f;
    private float glareSize = 5f;

    public void pushToShader(Shader shader) {
        pushToShader(shader, -1);
    }

    public void pushToShader(Shader shader, int index) {
        String prefix = index < 0 ? "light." : "lights[" + index + "].";
        pushToShader(shader, prefix);
    }

    protected void pushToShader(Shader shader, String prefix) {
        shader.setInt(prefix + "type", getType().ordinal());
        shader.setVec3(prefix + "pos", pos);
        shader.setVec3(prefix + "direction", dir);
        shader.setColor(prefix + "color", color);
        shader.setFloat(prefix + "intensity", intensity);
        shader.setMat4(prefix + "lightSpaceMatrix", lightSpaceMatrix);
    }

    public abstract void calculateLightSpaceMatrix();

    public abstract void copyTransform(Matrix4f matrix);

    protected abstract void updateAABB();

    public abstract Type getType();

    public boolean shouldRender(Camera camera) {
        return intensity > 0f && camera.getPos().distanceSquared(getPos()) <= 9216 && camera.isInsideFrustum(aabb); //96 * 96
    }

    protected void calculateLightViewMatrix() {
        float x = 0f, y = 1f, z = 0f;

        if (Math.abs(dir.dot(0f, 1f, 0f)) > 0.9999f) {
            float angle = Math.toRadians(getDirFallbackAngle() - 90f);
            x = Math.cos(angle); y = 0f; z = Math.sin(angle);
        }

        lightView.identity().lookAt(
                pos.x, pos.y, pos.z,
                pos.x + dir.x, pos.y + dir.y, pos.z + dir.z,
                x, y, z
        );
    }

    protected float getDirFallbackAngle() {
        return 0f;
    }

    public void renderDebug(Camera camera, MatrixStack matrices) {
        if (camera.getPos().distanceSquared(pos) <= 0.1f)
            return;

        matrices.pushMatrix();
        matrices.translate(pos);

        int c = color | 0xFF000000;
        if (getType() != Type.POINT) //point lights have no direction
            DebugScreen.renderDebugArrow(matrices, dir, 0.5f, c);

        camera.billboard(matrices);
        matrices.rotate(Rotation.Z.rotationDeg(180f));

        Vertex[] v = GeometryHelper.quad(matrices, -0.25f, -0.25f, 0.5f, 0.5f);
        VertexConsumer.MAIN.consume(v, LAMP);

        for (Vertex vertex : v)
            vertex.color(c);
        VertexConsumer.MAIN.consume(v, LAMP_OVERLAY);

        matrices.popMatrix();
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
        return castsShadows;
    }

    public Light castsShadows(boolean bool) {
        this.castsShadows = bool;
        return this;
    }

    public Mask getShadowMask() {
        return shadowMask;
    }

    public UUID getSource() {
        return source;
    }

    public Light source(UUID source) {
        this.source = source;
        return this;
    }

    public Resource getGlareTexture() {
        return glareTexture;
    }

    public Light glareTexture(Resource glareTexture) {
        this.glareTexture = glareTexture;
        return this;
    }

    public float getGlareIntensity() {
        return glareIntensity;
    }

    public Light glareIntensity(float glareIntensity) {
        this.glareIntensity = glareIntensity;
        return this;
    }

    public float getGlareSize() {
        return glareSize;
    }

    public Light glareSize(float glareSize) {
        this.glareSize = glareSize;
        return this;
    }

    public enum Type {
        POINT,
        SPOT,
        DIRECTIONAL,
        COOKIE
    }
}
