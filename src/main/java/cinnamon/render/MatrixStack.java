package cinnamon.render;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.Stack;

public class MatrixStack {
    private final Stack<Matrices> stack = new Stack<>() {{
        add(new Matrices(new Matrix4f(), new Matrix3f()));
    }};

    public MatrixStack pushMatrix() {
        Matrices mat = stack.peek();
        stack.push(new Matrices(new Matrix4f(mat.pos), new Matrix3f(mat.normal)));
        return this;
    }

    public MatrixStack popMatrix() {
        stack.pop();
        return this;
    }

    public Matrices peek() {
        return stack.peek();
    }

    public boolean isEmpty() {
        return stack.size() == 1;
    }

    public MatrixStack translate(float x, float y, float z) {
        stack.peek().pos.translate(x, y, z);
        return this;
    }

    public MatrixStack translate(Vector3f vector) {
        return translate(vector.x, vector.y, vector.z);
    }

    public MatrixStack translate(Vector3i vector) {
        return translate(vector.x, vector.y, vector.z);
    }

    public MatrixStack rotate(Quaternionf quaternion) {
        Matrices mat = stack.peek();
        mat.pos.rotate(quaternion);
        mat.normal.rotate(quaternion);
        return this;
    }

    public MatrixStack scale(float x, float y, float z) {
        Matrices mat = stack.peek();
        mat.pos.scale(x, y, z);

        if (x != y || y != z) {
            mat.normal.set(new Matrix3f(mat.pos).invert().transpose());
            if (mat.pos.determinant() < 0f)
                mat.normal.scale(-1f);
        }

        return this;
    }

    public MatrixStack scale(float scalar) {
        scale(scalar, scalar, scalar);
        return this;
    }

    public MatrixStack scale(Vector3f vector) {
        scale(vector.x, vector.y, vector.z);
        return this;
    }

    public MatrixStack identity() {
        Matrices mat = stack.peek();
        mat.pos.identity();
        mat.normal.identity();
        return this;
    }

    public record Matrices(Matrix4f pos, Matrix3f normal) {}
}
