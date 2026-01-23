package cinnamon.world;

import cinnamon.render.MatrixStack;
import org.joml.Math;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class Transform {

    protected final Vector3f
            pos = new Vector3f(),
            scale = new Vector3f(1f, 1f, 1f);
    protected final Quaternionf
            rot = new Quaternionf();

    protected final MatrixStack.Pose
            mat = new MatrixStack.Pose(),
            invMat = new MatrixStack.Pose();

    protected boolean dirty = false;

    public void applyTransform(MatrixStack target) {
        if (isDirty()) recalculate();
        target.peek().mul(mat);
    }

    protected void recalculatePose(MatrixStack.Pose mat) {
        mat.translate(pos);
        mat.rotate(rot);
        mat.scale(scale);
    }

    public Transform recalculate() {
        mat.identity();
        recalculatePose(mat);
        invMat.set(mat);
        invMat.pos().invert();
        invMat.recalculateNormalMatrix();
        dirty = false;
        return this;
    }

    public Transform identity() {
        pos.set(0f);
        rot.identity();
        scale.set(1f);
        mat.identity();
        invMat.identity();
        dirty = false;
        return this;
    }

    public Transform set(Transform o) {
        pos.set(o.pos);
        rot.set(o.rot);
        scale.set(o.scale);
        dirty = true;
        return this;
    }

    public Vector3f getPos() {
        return pos;
    }

    public Quaternionf getRot() {
        return rot;
    }

    public Vector3f getScale() {
        return scale;
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

    public boolean isDirty() {
        return dirty;
    }

    public MatrixStack.Pose getMatrix() {
        if (isDirty()) recalculate();
        return mat;
    }

    public MatrixStack.Pose getInverseMatrix() {
        if (isDirty()) recalculate();
        return invMat;
    }
}
