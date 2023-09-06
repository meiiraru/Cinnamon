package mayo.render;

import mayo.utils.Rotation;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class Camera {

    public static final float FOV = 70f;

    private final Vector3f pos = new Vector3f();
    private final Vector3f forwards = new Vector3f(0f, 0f, -1f);
    private final Vector3f up = new Vector3f(0f, 1f, 0f);
    private final Vector3f left = new Vector3f(1f, 0f, 0f);

    private float xRot, yRot;
    private final Quaternionf rotation = new Quaternionf();

    private final Matrix4f viewMatrix = new Matrix4f();

    public void move(float x, float y, float z) {
        pos.add(new Vector3f(x, y, z).rotate(new Quaternionf().rotationY((float) Math.toRadians(-yRot))));
        calculateMatrix();
    }

    public void rotate(float yaw, float pitch) {
        this.xRot = pitch;
        this.yRot = yaw;
        this.rotation.rotationYXZ((float) Math.toRadians(-yaw), (float) Math.toRadians(-pitch), 0f);
        this.forwards.set(0f, 0f, -1f).rotate(this.rotation);
        this.up.set(0f, 1f, 0f).rotate(this.rotation);
        this.left.set(1f, 0f, 0f).rotate(this.rotation);
        calculateMatrix();
    }

    private void calculateMatrix() {
        viewMatrix.identity();
        viewMatrix.rotate(Rotation.X.rotationDeg(xRot));
        viewMatrix.rotate(Rotation.Y.rotationDeg(yRot));
        //matrix.rotate(Rotation.Z.rotationDeg(180f));
        viewMatrix.translate(-pos.x, -pos.y, -pos.z);
    }

    public void billboard(Matrix4f matrix) {
        verticalBillboard(matrix);
        horizontalBillboard(matrix);
    }

    public void horizontalBillboard(Matrix4f matrix) {
        matrix.rotate(Rotation.X.rotationDeg(xRot));
    }

    public void verticalBillboard(Matrix4f matrix) {
        matrix.rotate(Rotation.Y.rotationDeg(180f - yRot));
    }

    public boolean isInsideFrustum(float x, float y, float z) {
        Vector3f facing = getForwards();
        Vector3f target = new Vector3f(x, y, z).sub(pos).normalize();
        return facing.dot(target) > Math.cos(Math.toRadians(FOV));
    }

    public Vector3f getPos() {
        return new Vector3f(pos);
    }

    public Vector3f getForwards() {
        return new Vector3f(forwards);
    }

    public Vector3f getLeft() {
        return new Vector3f(left);
    }

    public Vector3f getUp() {
        return new Vector3f(up);
    }

    public Matrix4f getMatrix() {
        return viewMatrix;
    }
}
