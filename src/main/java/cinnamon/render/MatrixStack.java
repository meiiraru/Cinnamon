package cinnamon.render;

import org.joml.Math;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Stack;

public class MatrixStack {
    private final Stack<Pose> stack = new Stack<>() {{
        add(new Pose());
    }};

    public MatrixStack pushMatrix() {
        Pose mat = stack.peek();
        stack.push(new Pose(mat.pos, mat.normal));
        return this;
    }

    public MatrixStack popMatrix() {
        stack.pop();
        return this;
    }

    public Pose peek() {
        return stack.peek();
    }

    public boolean isEmpty() {
        return stack.size() == 1;
    }

    public MatrixStack translate(float x, float y, float z) {
        stack.peek().translate(x, y, z);
        return this;
    }

    public MatrixStack translate(Vector3f vector) {
        stack.peek().translate(vector);
        return this;
    }

    public MatrixStack rotate(Quaternionf quaternion) {
        stack.peek().rotate(quaternion);
        return this;
    }

    public MatrixStack scale(float x, float y, float z) {
        stack.peek().scale(x, y, z);
        return this;
    }

    public MatrixStack scale(float scalar) {
        stack.peek().scale(scalar);
        return this;
    }

    public MatrixStack scale(Vector3f vector) {
        stack.peek().scale(vector);
        return this;
    }

    public MatrixStack identity() {
        stack.peek().identity();
        return this;
    }

    public MatrixStack mul(Matrix4f pos, Matrix3f normal) {
        stack.peek().mul(pos, normal);
        return this;
    }

    public static class Pose {

        private final Matrix4f pos = new Matrix4f();
        private final Matrix3f normal = new Matrix3f();

        public Pose() {}

        public Pose(Matrix4f pos, Matrix3f normal) {
            this.pos.set(pos);
            this.normal.set(normal);
        }

        public Pose translate(float x, float y, float z) {
            pos.translate(x, y, z);
            return this;
        }

        public Pose translate(Vector3f vector) {
            return translate(vector.x, vector.y, vector.z);
        }

        public Pose rotate(Quaternionf quaternion) {
            pos.rotate(quaternion);
            normal.rotate(quaternion);
            return this;
        }

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

        public Pose scale(float scalar) {
            return scale(scalar, scalar, scalar);
        }

        public Pose scale(Vector3f vector) {
            return scale(vector.x, vector.y, vector.z);
        }

        public Pose identity() {
            pos.identity();
            normal.identity();
            return this;
        }

        public Pose set(Pose other) {
            return set(other.pos, other.normal);
        }

        public Pose set(Matrix4f pos, Matrix3f normal) {
            this.pos.set(pos);
            this.normal.set(normal);
            return this;
        }

        public Pose set(Matrix4f pos) {
            this.pos.set(pos);
            return recalculateNormalMatrix();
        }

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

        public Pose mul(Pose other) {
            return mul(other.pos, other.normal);
        }

        public Pose mul(Matrix4f pos, Matrix3f normal) {
            this.pos.mul(pos);
            this.normal.mul(normal);
            return this;
        }

        public Matrix4f pos() {
            return pos;
        }

        public Matrix3f normal() {
            return normal;
        }
    }
}
