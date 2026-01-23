package cinnamon.model;

import cinnamon.render.MatrixStack;
import cinnamon.world.Transform;
import org.joml.Math;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class ModelTransform extends Transform {

    protected final Vector3f pivot = new Vector3f();
    protected final Quaternionf pivotRot = new Quaternionf();
    protected final Vector4f color = new Vector4f(1f, 1f, 1f, 1f);
    protected final Vector2f uv = new Vector2f();

    @Override
    protected void recalculatePose(MatrixStack.Pose mat) {
        mat.translate(pivot.x + pos.x, pivot.y + pos.y, pivot.z + pos.z);
        mat.rotate(rot);
        mat.rotate(pivotRot);
        mat.scale(scale);
        mat.translate(-pivot.x, -pivot.y, -pivot.z);
    }

    public ModelTransform clearAnimationPose() {
        pos.set(0f);
        rot.identity();
        scale.set(1f);
        dirty = true;
        return this;
    }

    @Override
    public Transform identity() {
        pivot.set(0f);
        pivotRot.identity();
        color.set(1f);
        uv.set(0f);
        return super.identity();
    }

    @Override
    public Transform set(Transform o) {
        if (o instanceof ModelTransform m) {
            pivot.set(m.pivot);
            pivotRot.set(m.pivotRot);
            color.set(m.color);
            uv.set(m.uv);
        }
        return super.set(o);
    }

    public Vector3f getPivot() {
        return pivot;
    }

    public Quaternionf getPivotRot() {
        return pivotRot;
    }

    public Vector4f getColor() {
        return color;
    }

    public Vector2f getUV() {
        return uv;
    }

    public Transform setPivot(Vector3f vec) {
        return this.setPivot(vec.x, vec.y, vec.z);
    }

    public Transform setPivot(float x, float y, float z) {
        this.pivot.set(x, y, z);
        this.dirty = true;
        return this;
    }

    public Transform setPivotRot(Quaternionf rot) {
        this.pivotRot.set(rot);
        this.dirty = true;
        return this;
    }

    public Transform setPivotRot(Vector3f vec) {
        return this.setPivotRot(vec.x, vec.y, vec.z);
    }

    public Transform setPivotRot(float pitch, float yaw, float roll) {
        this.pivotRot.rotationZYX(Math.toRadians(roll), Math.toRadians(-yaw), Math.toRadians(-pitch));
        this.dirty = true;
        return this;
    }

    public Transform setColor(Vector3f vec) {
        return this.setColor(vec.x, vec.y, vec.z);
    }

    public Transform setColor(float r, float g, float b) {
        return this.setColor(r, g, b, 1f);
    }

    public Transform setColor(Vector4f vec) {
        return this.setColor(vec.x, vec.y, vec.z, vec.w);
    }

    public Transform setColor(float r, float g, float b, float a) {
        this.color.set(r, g, b, a);
        return this;
    }

    public Transform setUV(Vector2f vec) {
        return this.setUV(vec.x, vec.y);
    }

    public Transform setUV(float x, float y) {
        this.uv.set(x, y);
        return this;
    }
}
