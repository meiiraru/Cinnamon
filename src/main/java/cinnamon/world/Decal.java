package cinnamon.world;

import cinnamon.gui.DebugScreen;
import cinnamon.model.GeometryHelper;
import cinnamon.model.Transform;
import cinnamon.render.Camera;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.utils.AABB;
import cinnamon.utils.Resource;
import org.joml.Matrix4f;

public class Decal {

    private final Transform transform = new Transform();
    private final Resource albedoTexture;
    private final AABB aabb = new AABB().inflate(0.5f);

    private final int lifetime;
    private int age;

    private int fadeOutTime = 20;

    public Decal(int lifetime, Resource albedoTexture) {
        this.lifetime = lifetime;
        this.albedoTexture = albedoTexture;
    }

    public void tick() {
        age++;
    }

    public void renderDebugHitbox(MatrixStack matrices, float delta) {
        //aabb area
        AABB aabb = getAABB();
        VertexConsumer.LINES.consume(GeometryHelper.box(matrices, aabb.minX(), aabb.minY(), aabb.minZ(), aabb.maxX(), aabb.maxY(), aabb.maxZ(), 0xFFFFFFFF));

        //transform box
        matrices.pushMatrix();
        getTransform().applyTransform(matrices.peek());
        VertexConsumer.LINES.consume(GeometryHelper.box(matrices, -0.5f, -0.5f, -0.5f, 0.5f, 0.5f, 0.5f, 0xFFFF72AD));
        DebugScreen.renderDebugArrow(matrices, 0, 0, 1, 1f, 0xFF0000FF);
        matrices.popMatrix();
    }

    public Resource getAlbedoTexture() {
        return albedoTexture;
    }

    public Transform getTransform() {
        return transform;
    }

    public Matrix4f getModelMatrix() {
        return transform.getMatrix().pos();
    }

    public Matrix4f getInverseModelMatrix() {
        return transform.getInverseMatrix().pos();
    }

    public boolean isRemoved() {
        return age >= lifetime;
    }

    public void setFadeOutTime(int fadeOutTime) {
        this.fadeOutTime = fadeOutTime;
    }

    public int getFadeOutTime() {
        return fadeOutTime;
    }

    public float getOpacity() {
        if (age >= lifetime - fadeOutTime)
            return 1f - (float) (age - (lifetime - fadeOutTime)) / fadeOutTime;
        return 1f;
    }

    public AABB getAABB() {
        if (!transform.isDirty())
            return aabb;

        return aabb.applyMatrix(getTransform().getMatrix().pos());
    }

    public boolean shouldRender(Camera camera) {
        return camera.isInsideFrustum(getAABB());
    }
}
