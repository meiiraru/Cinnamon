package cinnamon.math;

import cinnamon.render.MatrixStack;
import org.joml.Math;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * A class that represents a 3D transformation, including position, rotation, and scale<br>
 * It provides methods to apply the transformation to a matrix stack and to recalculate the transformation matrix
 */
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

    /**
     * Applies this transformation to the given {@link MatrixStack}
     * @param target The {@link MatrixStack} to apply the transformation to
     */
    public void applyTransform(MatrixStack target) {
        if (isDirty()) recalculate();
        target.peek().mul(mat);
    }

    /**
     * Recalculates the transformation matrix based on the current position, rotation, and scale
     * @param mat The {@link MatrixStack.Pose} to store the recalculated transformation matrix
     */
    protected void recalculatePose(MatrixStack.Pose mat) {
        mat.translate(pos);
        mat.rotate(rot);
        mat.scale(scale);
    }

    /**
     * Recalculates the transformation matrix and its inverse based on the current position, rotation, and scale
     * @return This {@link Transform} instance
     */
    public Transform recalculate() {
        mat.identity();
        recalculatePose(mat);
        invMat.set(mat);
        invMat.pos().invert();
        invMat.recalculateNormalMatrix();
        dirty = false;
        return this;
    }

    /**
     * Resets the transformation to the identity transformation (no translation, no rotation, and unit scale)
     * @return This {@link Transform} instance
     */
    public Transform identity() {
        pos.set(0f);
        rot.identity();
        scale.set(1f);
        mat.identity();
        invMat.identity();
        dirty = false;
        return this;
    }

    /**
     * Sets this transformation to copy another {@link Transform} instance
     * @param o The other {@link Transform} instance to copy from
     * @return This {@link Transform} instance
     */
    public Transform set(Transform o) {
        pos.set(o.pos);
        rot.set(o.rot);
        scale.set(o.scale);
        dirty = true;
        return this;
    }

    /**
     * Gets the position of this transformation
     * @return The position as a {@link Vector3f}
     */
    public Vector3f getPos() {
        return pos;
    }

    /**
     * Gets the rotation of this transformation
     * @return The rotation as a {@link Quaternionf}
     */
    public Quaternionf getRot() {
        return rot;
    }

    /**
     * Gets the scale of this transformation
     * @return The scale as a {@link Vector3f}
     */
    public Vector3f getScale() {
        return scale;
    }

    /**
     * Sets the position of this transformation
     * @param vec The {@link Vector3f} position
     * @return This {@link Transform} instance
     * @see #setPos(float, float, float)
     */
    public Transform setPos(Vector3f vec) {
        return this.setPos(vec.x, vec.y, vec.z);
    }

    /**
     * Sets the position of this transformation
     * @param x The x coordinate of the position
     * @param y The y coordinate of the position
     * @param z The z coordinate of the position
     * @return This {@link Transform} instance
     * @see #setPos(Vector3f)
     */
    public Transform setPos(float x, float y, float z) {
        this.pos.set(x, y, z);
        this.dirty = true;
        return this;
    }

    /**
     * Sets the rotation of this transformation
     * @param rot The {@link Quaternionf} rotation
     * @return This {@link Transform} instance
     * @see #setRot(float, float, float)
     */
    public Transform setRot(Quaternionf rot) {
        this.rot.set(rot);
        this.dirty = true;
        return this;
    }

    /**
     * Sets the rotation of this transformation using Euler angles {@code pitch, yaw, roll} in degrees
     * @param vec The {@link Vector3f} containing the Euler angles in degrees
     * @return This {@link Transform} instance
     * @see #setRot(float, float, float)
     */
    public Transform setRot(Vector3f vec) {
        return this.setRot(vec.x, vec.y, vec.z);
    }

    /**
     * Sets the rotation of this transformation using Euler angles {@code pitch, yaw, roll} in degrees
     * @param pitch The pitch angle in degrees
     * @param yaw The yaw angle in degrees
     * @param roll The roll angle in degrees
     * @return This {@link Transform} instance
     * @see #setRot(Vector3f)
     */
    public Transform setRot(float pitch, float yaw, float roll) {
        this.rot.rotationZYX(Math.toRadians(roll), Math.toRadians(-yaw), Math.toRadians(-pitch));
        this.dirty = true;
        return this;
    }

    /**
     * Sets the scale of this transformation uniformly in all directions
     * @param scalar The uniform scale factor
     * @return This {@link Transform} instance
     * @see #setScale(float, float, float)
     */
    public Transform setScale(float scalar) {
        return this.setScale(scalar, scalar, scalar);
    }

    /**
     * Sets the scale of this transformation
     * @param vec The {@link Vector3f} containing the scale factors for each axis
     * @return This {@link Transform} instance
     * @see #setScale(float, float, float)
     */
    public Transform setScale(Vector3f vec) {
        return this.setScale(vec.x, vec.y, vec.z);
    }

    /**
     * Sets the scale of this transformation
     * @param x The scale factor along the x axis
     * @param y The scale factor along the y axis
     * @param z The scale factor along the z axis
     * @return This {@link Transform} instance
     * @see #setScale(float)
     * @see #setScale(Vector3f)
     */
    public Transform setScale(float x, float y, float z) {
        this.scale.set(x, y, z);
        this.dirty = true;
        return this;
    }

    /**
     * Flags this transformation as dirty, indicating that the transformation matrix needs to be recalculated
     */
    public void markDirty() {
        this.dirty = true;
    }

    /**
     * Checks if this transformation is dirty, indicating that the transformation matrix needs to be recalculated
     * @return {@code true} if the transformation is dirty, {@code false} otherwise
     */
    public boolean isDirty() {
        return dirty;
    }

    /**
     * Gets the transformation matrix of this transformation
     * @return The transformation matrix as a {@link MatrixStack.Pose}
     */
    public MatrixStack.Pose getMatrix() {
        if (isDirty()) recalculate();
        return mat;
    }

    /**
     * Gets the inverse transformation matrix of this transformation
     * @return The inverse transformation matrix as a {@link MatrixStack.Pose}
     */
    public MatrixStack.Pose getInverseMatrix() {
        if (isDirty()) recalculate();
        return invMat;
    }
}
