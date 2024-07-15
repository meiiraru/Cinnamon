package cinnamon.render;

import cinnamon.utils.Maths;
import org.joml.*;

import java.util.Stack;

public class MatrixStack {
    private final Stack<Matrices> stack = new Stack<>() {{
        add(new Matrices(new Matrix4f(), new Matrix3f()));
    }};

    public void push() {
        Matrices mat = stack.peek();
        stack.push(new Matrices(new Matrix4f(mat.pos), new Matrix3f(mat.normal)));
    }

    public void pop() {
        stack.pop();
    }

    public void popPush() {
        pop();
        push();
    }

    public Matrices peek() {
        return stack.peek();
    }

    public boolean isEmpty() {
        return stack.size() == 1;
    }

    public void translate(float x, float y, float z) {
        stack.peek().pos.translate(x, y, z);
    }

    public void translate(Vector3f vector) {
        translate(vector.x, vector.y, vector.z);
    }

    public void translate(Vector3i vector) {
        translate(vector.x, vector.y, vector.z);
    }

    public void rotate(Quaternionf quaternion) {
        Matrices mat = stack.peek();
        mat.pos.rotate(quaternion);
        mat.normal.rotate(quaternion);
    }

    public void scale(float x, float y, float z) {
        Matrices mat = stack.peek();
        mat.pos.scale(x, y, z);
        if (x == y && y == z) {
            if (x > 0f)
                return;

            mat.normal.scale(-1f);
        }

        float rX = 1f / x, rY = 1f / y, rZ = 1f / z;
        float i = Maths.fastInvCubeRoot(rX * rY * rZ);
        mat.normal.scale(i * rX, i * rY, i * rZ);
    }

    public void scale(float scalar) {
        scale(scalar, scalar, scalar);
    }

    public void identity() {
        Matrices mat = stack.peek();
        mat.pos.identity();
        mat.normal.identity();
    }

    public void mulPos(Matrix4f matrix) {
        stack.peek().pos.mul(matrix);
    }

    public record Matrices(Matrix4f pos, Matrix3f normal) {}
}
