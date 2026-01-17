package cinnamon.model;

import cinnamon.render.MatrixStack;
import org.joml.Math;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class Transform {

    private final Vector4f
            color = new Vector4f(1f, 1f, 1f, 1f);
    private final Vector3f
            pos = new Vector3f(),
            pivot = new Vector3f(),
            scale = new Vector3f(1f, 1f, 1f);
    private final Vector2f
            uv = new Vector2f();
    private final Quaternionf
            rot = new Quaternionf();
    private final MatrixStack.Matrices
            mat = new MatrixStack.Matrices();

    protected boolean dirty = false;

    public Transform applyTransform(MatrixStack.Matrices matrices) {
        matrices.translate(pivot.x + pos.x, pivot.y + pos.y, pivot.z + pos.z);
        matrices.rotate(rot);
        matrices.scale(scale);
        matrices.translate(-pivot.x, -pivot.y, -pivot.z);
        return this;
    }

    public Transform recalculate() {
        mat.identity();
        applyTransform(mat);
        dirty = false;
        return this;
    }

    public Transform clearAnimChannels() {
        scale.set(1f);
        rot  .identity();
        pos  .set(0f);
        dirty = true;
        return this;
    }

    public Transform identity() {
        pivot.set(0f);
        pos  .set(0f);
        rot  .identity();
        scale.set(1f);
        uv   .set(0f);
        color.set(1f);
        mat.identity();
        dirty = false;
        return this;
    }

    public Transform set(Transform o) {
        pivot.set(o.pivot);
        pos  .set(o.pos);
        rot  .set(o.rot);
        scale.set(o.scale);
        uv   .set(o.uv);
        color.set(o.color);
        dirty = true;
        return this;
    }

    public Vector3f getPos() {
        return pos;
    }

    public Quaternionf getRot() {
        return rot;
    }

    public Vector3f getPivot() {
        return pivot;
    }

    public Vector3f getScale() {
        return scale;
    }

    public Vector4f getColor() {
        return color;
    }

    public Vector2f getUV() {
        return uv;
    }

    public Transform setPos(Vector3f vec) {
        return this.setPos(vec.x, vec.y, vec.z);
    }

    public Transform setPos(float x, float y, float z) {
        this.pos.set(x, y, z);
        this.dirty = true;
        return this;
    }

    public Transform setRot(Quaternionf rot) {
        this.rot.set(rot);
        this.dirty = true;
        return this;
    }

    public Transform setRot(Vector3f vec) {
        return this.setRot(vec.x, vec.y, vec.z);
    }

    public Transform setRot(float pitch, float yaw, float roll) {
        this.rot.rotationZYX(Math.toRadians(roll), Math.toRadians(-yaw), Math.toRadians(-pitch));
        this.dirty = true;
        return this;
    }

    public Transform setPivot(Vector3f vec) {
        return this.setPivot(vec.x, vec.y, vec.z);
    }

    public Transform setPivot(float x, float y, float z) {
        this.pivot.set(x, y, z);
        this.dirty = true;
        return this;
    }

    public Transform setPosPivot(Vector3f vec) {
        return this.setPosPivot(vec.x, vec.y, vec.z);
    }

    public Transform setPosPivot(float x, float y, float z) {
        setPos(x, y, z);
        setPivot(x, y, z);
        return this;
    }

    public Transform setScale(float scalar) {
        return this.setScale(scalar, scalar, scalar);
    }

    public Transform setScale(Vector3f vec) {
        return this.setScale(vec.x, vec.y, vec.z);
    }

    public Transform setScale(float x, float y, float z) {
        this.scale.set(x, y, z);
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

    public boolean isDirty() {
        return dirty;
    }

    public MatrixStack.Matrices getMatrix() {
        if (isDirty()) recalculate();
        return mat;
    }
}
