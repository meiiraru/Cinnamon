package cinnamon.math;

import org.joml.Math;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Stack;

/**
 * A {@link Stack} of {@link Pose} objects that can be used to store and manipulate transformation matrices
 */
public class MatrixStack {
    private final Stack<Pose> stack = new Stack<>() {{
        add(new Pose());
    }};

    /**
     * Pushes a new {@link Pose} onto the stack, copying the current top {@link Pose}
     * @return this {@link MatrixStack} instance
     */
    public MatrixStack pushMatrix() {
        Pose mat = stack.peek();
        stack.push(new Pose(mat.pos, mat.normal));
        return this;
    }

    /**
     * Pops the top {@link Pose} off the stack, returning to the previous {@link Pose} state
     * @return this {@link MatrixStack} instance
     */
    public MatrixStack popMatrix() {
        stack.pop();
        return this;
    }

    /**
     * Returns the current top {@link Pose} on the stack without removing it
     * @return the current {@link Pose} on the stack
     */
    public Pose peek() {
        return stack.peek();
    }

    /**
     * Checks if the stack is empty
     * @return {@code true} if the stack is empty, {@code false} otherwise
     */
    public boolean isEmpty() {
        return stack.size() == 1;
    }

    /**
     * Applies a translation to the current {@link Pose} on the stack
     * @param x the translation along the x-axis
     * @param y the translation along the y-axis
     * @param z the translation along the z-axis
     * @return this {@link MatrixStack} instance
     * @see #translate(float, float, float)
     * @see #translate(Vector3f)
     */
    public MatrixStack translate(float x, float y, float z) {
        stack.peek().translate(x, y, z);
        return this;
    }

    /**
     * Applies a translation to the current {@link Pose} on the stack
     * @param vector the translation vector
     * @return this {@link MatrixStack} instance
     * @see #translate(float, float, float)
     * @see #translate(Vector3f)
     */
    public MatrixStack translate(Vector3f vector) {
        stack.peek().translate(vector);
        return this;
    }

    /**
     * Applies a {@link Quaternionf} rotation to the current {@link Pose} on the stack
     * @param quaternion the rotation quaternion
     * @return this {@link MatrixStack} instance
     * @see #rotateX(float)
     * @see #rotateY(float)
     * @see #rotateZ(float)
     */
    public MatrixStack rotate(Quaternionf quaternion) {
        stack.peek().rotate(quaternion);
        return this;
    }

    /**
     * Applies a rotation around the x-axis to the current {@link Pose} on the stack
     * @param angle the rotation angle in degrees
     * @return this {@link MatrixStack} instance
     * @see #rotate(Quaternionf)
     * @see #rotateY(float)
     * @see #rotateZ(float)
     */
    public MatrixStack rotateX(float angle) {
        stack.peek().rotateX(angle);
        return this;
    }

    /**
     * Applies a rotation around the y-axis to the current {@link Pose} on the stack
     * @param angle the rotation angle in degrees
     * @return this {@link MatrixStack} instance
     * @see #rotate(Quaternionf)
     * @see #rotateX(float)
     * @see #rotateZ(float)
     */
    public MatrixStack rotateY(float angle) {
        stack.peek().rotateY(angle);
        return this;
    }

    /**
     * Applies a rotation around the z-axis to the current {@link Pose} on the stack
     * @param angle the rotation angle in degrees
     * @return this {@link MatrixStack} instance
     * @see #rotate(Quaternionf)
     * @see #rotateX(float)
     * @see #rotateY(float)
     */
    public MatrixStack rotateZ(float angle) {
        stack.peek().rotateZ(angle);
        return this;
    }

    /**
     * Applies a scaling transformation to the current {@link Pose} on the stack
     * @param x the scaling factor along the x-axis
     * @param y the scaling factor along the y-axis
     * @param z the scaling factor along the z-axis
     * @return this {@link MatrixStack} instance
     * @see #scale(float)
     * @see #scale(Vector3f)
     */
    public MatrixStack scale(float x, float y, float z) {
        stack.peek().scale(x, y, z);
        return this;
    }

    /**
     * Applies a uniform scaling transformation to the current {@link Pose} on the stack
     * @param scalar the uniform scaling factor
     * @return this {@link MatrixStack} instance
     * @see #scale(float, float, float)
     * @see #scale(Vector3f)
     */
    public MatrixStack scale(float scalar) {
        stack.peek().scale(scalar);
        return this;
    }

    /**
     * Applies a scaling transformation to the current {@link Pose} on the stack using a vector
     * @param vector the scaling vector
     * @return this {@link MatrixStack} instance
     * @see #scale(float, float, float)
     * @see #scale(float)
     */
    public MatrixStack scale(Vector3f vector) {
        stack.peek().scale(vector);
        return this;
    }

    /**
     * Resets the current {@link Pose} on the stack to the identity transformation
     * @return this {@link MatrixStack} instance
     */
    public MatrixStack identity() {
        stack.peek().identity();
        return this;
    }

    /**
     * Resets the rotation component of the current {@link Pose} on the stack to the identity transformation
     * @return this {@link MatrixStack} instance
     * @see #identityScale()
     * @see #identityTranslation()
     */
    public MatrixStack identityRotation() {
        stack.peek().identityRotation();
        return this;
    }

    /**
     * Resets the scaling component of the current {@link Pose} on the stack to the identity transformation
     * @return this {@link MatrixStack} instance
     * @see #identityRotation()
     * @see #identityTranslation()
     */
    public MatrixStack identityScale() {
        stack.peek().identityScale();
        return this;
    }

    /**
     * Resets the translation component of the current {@link Pose} on the stack to the identity transformation
     * @return this {@link MatrixStack} instance
     * @see #identityRotation()
     * @see #identityScale()
     */
    public MatrixStack identityTranslation() {
        stack.peek().identityTranslation();
        return this;
    }

    /**
     * Sets the current {@link Pose} on the stack to the specified {@link Pose}
     * @param other the {@link Pose} to set
     * @return this {@link MatrixStack} instance
     */
    public MatrixStack set(Pose other) {
        stack.peek().set(other);
        return this;
    }

    /**
     * Multiplies the current {@link Pose} on the stack with the specified position and normal matrices
     * @param pos the position matrix
     * @param normal the normal matrix
     * @return this {@link MatrixStack} instance
     */
    public MatrixStack mul(Matrix4f pos, Matrix3f normal) {
        stack.peek().mul(pos, normal);
        return this;
    }

    /**
     * A Pose object that represents a transformation in 3D space, consisting of a position {@link Matrix4f} and a normal {@link Matrix3f}<br>
     * All the operations on the Pose object will modify both the position and normal matrices accordingly
     */
    public static class Pose {

        private final Matrix4f pos = new Matrix4f();
        private final Matrix3f normal = new Matrix3f();

        /**
         * Identity constructor
         */
        public Pose() {}

        /**
         * Constructor that initializes the Pose with the given position and normal matrices
         * @param pos the position matrix
         * @param normal the normal matrix
         */
        public Pose(Matrix4f pos, Matrix3f normal) {
            this.pos.set(pos);
            this.normal.set(normal);
        }

        /**
         * Translates the Pose by the given x, y, and z values
         * @param x the translation along the x-axis
         * @param y the translation along the y-axis
         * @param z the translation along the z-axis
         * @return this Pose instance
         * @see #translate(Vector3f)
         */
        public Pose translate(float x, float y, float z) {
            pos.translate(x, y, z);
            return this;
        }

        /**
         * Translates the Pose by the given vector
         * @param vector the translation vector
         * @return this Pose instance
         * @see #translate(float, float, float)
         */
        public Pose translate(Vector3f vector) {
            return translate(vector.x, vector.y, vector.z);
        }

        /**
         * Rotates the Pose by the given quaternion
         * @param quaternion the rotation quaternion
         * @return this Pose instance
         * @see #rotateX(float)
         * @see #rotateY(float)
         * @see #rotateZ(float)
         */
        public Pose rotate(Quaternionf quaternion) {
            pos.rotate(quaternion);
            normal.rotate(quaternion);
            return this;
        }

        /**
         * Rotates the Pose around the x-axis by the given angle in degrees
         * @param angle the rotation angle in degrees
         * @return this Pose instance
         * @see #rotate(Quaternionf)
         * @see #rotateY(float)
         * @see #rotateZ(float)
         */
        public Pose rotateX(float angle) {
            float ang = Math.toRadians(angle);
            pos.rotateX(ang);
            normal.rotateX(ang);
            return this;
        }

        /**
         * Rotates the Pose around the y-axis by the given angle in degrees
         * @param angle the rotation angle in degrees
         * @return this Pose instance
         * @see #rotate(Quaternionf)
         * @see #rotateX(float)
         * @see #rotateZ(float)
         */
        public Pose rotateY(float angle) {
            float ang = Math.toRadians(angle);
            pos.rotateY(ang);
            normal.rotateY(ang);
            return this;
        }

        /**
         * Rotates the Pose around the z-axis by the given angle in degrees
         * @param angle the rotation angle in degrees
         * @return this Pose instance
         * @see #rotate(Quaternionf)
         * @see #rotateX(float)
         * @see #rotateY(float)
         */
        public Pose rotateZ(float angle) {
            float ang = Math.toRadians(angle);
            pos.rotateZ(ang);
            normal.rotateZ(ang);
            return this;
        }

        /**
         * Scales the Pose by the given x, y, and z values<br>
         * If the scaling is non-uniform, the normal matrix will be recalculated accordingly
         * @param x the scaling factor along the x-axis
         * @param y the scaling factor along the y-axis
         * @param z the scaling factor along the z-axis
         * @return this Pose instance
         * @see #scale(float)
         * @see #scale(Vector3f)
         */
        public Pose scale(float x, float y, float z) {
            pos.scale(x, y, z);

            if (Math.abs(x) == Math.abs(y) && Math.abs(y) == Math.abs(z)) {
                if (x < 0f || y < 0f || z < 0f) {
                    float signX = Math.signum(x);
                    float signY = Math.signum(y);
                    float signZ = Math.signum(z);
                    normal.scale(signX, signY, signZ);

                    float det = signX * signY * signZ;
                    if (det < 0f)
                        normal.scale(-1f);
                }
            } else {
                recalculateNormalMatrix();
            }

            return this;
        }

        /**
         * Scales the Pose uniformly by the given scalar value
         * @param scalar the uniform scaling factor
         * @return this Pose instance
         * @see #scale(float, float, float)
         * @see #scale(Vector3f)
         */
        public Pose scale(float scalar) {
            return scale(scalar, scalar, scalar);
        }

        /**
         * Scales the Pose by the given vector<br>
         * If the scaling is non-uniform, the normal matrix will be recalculated accordingly
         * @param vector the scaling vector
         * @return this Pose instance
         * @see #scale(float, float, float)
         * @see #scale(float)
         */
        public Pose scale(Vector3f vector) {
            return scale(vector.x, vector.y, vector.z);
        }

        /**
         * Resets the Pose to the identity transformation
         * @return this Pose instance
         */
        public Pose identity() {
            pos.identity();
            normal.identity();
            return this;
        }

        /**
         * Resets the rotation component of the Pose to the identity transformation
         * @return this Pose instance
         * @see #identityScale()
         * @see #identityTranslation()
         */
        public Pose identityRotation() {
            Maths.removeRotation(pos);
            Maths.removeRotation(normal);
            return this;
        }

        /**
         * Resets the scaling component of the Pose to the identity transformation
         * @return this Pose instance
         * @see #identityRotation()
         * @see #identityTranslation()
         */
        public Pose identityScale() {
            pos.normalize3x3();
            normal.normal();
            return this;
        }

        /**
         * Resets the translation component of the Pose to the identity transformation
         * @return this Pose instance
         * @see #identityRotation()
         * @see #identityScale()
         */
        public Pose identityTranslation() {
            pos.setTranslation(0f, 0f, 0f);
            return this;
        }

        /**
         * Sets the Pose to the specified Pose
         * @param other the Pose to set
         * @return this Pose instance
         */
        public Pose set(Pose other) {
            return set(other.pos, other.normal);
        }

        /**
         * Sets the Pose to the specified position and normal matrices
         * @param pos the position matrix
         * @param normal the normal matrix
         * @return this Pose instance
         */
        public Pose set(Matrix4f pos, Matrix3f normal) {
            this.pos.set(pos);
            this.normal.set(normal);
            return this;
        }

        /**
         * Sets the Pose to the specified position matrix and recalculates the normal matrix accordingly
         * @param pos the position matrix
         * @return this Pose instance
         */
        public Pose set(Matrix4f pos) {
            this.pos.set(pos);
            return recalculateNormalMatrix();
        }

        /**
         * Recalculates the normal matrix based on the current position matrix<br>
         * If the determinant of the position matrix is zero, the normal matrix will be set to the identity matrix
         * @return this Pose instance
         */
        public Pose recalculateNormalMatrix() {
            float det = pos.determinant();
            if (det == 0f) {
                normal.identity();
            } else {
                normal.set(pos).invert().transpose();
                if (det < 0f)
                    normal.scale(-1f);
            }
            return this;
        }

        /**
         * Multiplies the Pose with the specified Pose
         * @param other the Pose to multiply with
         * @return this Pose instance
         */
        public Pose mul(Pose other) {
            return mul(other.pos, other.normal);
        }

        /**
         * Multiplies the Pose with the specified position and normal matrices
         * @param pos the position matrix
         * @param normal the normal matrix
         * @return this Pose instance
         */
        public Pose mul(Matrix4f pos, Matrix3f normal) {
            this.pos.mul(pos);
            this.normal.mul(normal);
            return this;
        }

        /**
         * Returns the position matrix of the Pose
         * @return the position matrix
         */
        public Matrix4f pos() {
            return pos;
        }

        /**
         * Returns the normal matrix of the Pose
         * @return the normal matrix
         */
        public Matrix3f normal() {
            return normal;
        }
    }
}
