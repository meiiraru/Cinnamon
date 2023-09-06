package mayo.render;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.Stack;

public class MatrixStack {
    private final Stack<Matrix4f> stack = new Stack<>() {{
        add(new Matrix4f());
    }};

    public void push() {
        stack.push(new Matrix4f(stack.peek()));
    }

    public void pop() {
        stack.pop();
    }

    public void popPush() {
        pop();
        push();
    }

    public Matrix4f peek() {
        return stack.peek();
    }

    public boolean isEmpty() {
        return stack.size() == 1;
    }

    public void translate(float x, float y, float z) {
        stack.peek().translate(x, y, z);
    }

    public void translate(Vector3f vector) {
        translate(vector.x, vector.y, vector.z);
    }

    public void translate(Vector3i vector) {
        translate(vector.x, vector.y, vector.z);
    }

    public void rotate(Quaternionf quaternion) {
        stack.peek().rotate(quaternion);
    }

    public void scale(float x, float y, float z) {
        stack.peek().scale(x, y, z);
    }

    public void scale(float scalar) {
        scale(scalar, scalar, scalar);
    }

    public void mul(Matrix4f mat) {
        stack.peek().mul(mat);
    }

    public void setIdentity() {
        stack.peek().identity();
    }
}
