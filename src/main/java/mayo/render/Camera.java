package mayo.render;

import mayo.utils.Meth;
import mayo.utils.Rotation;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class Camera {

    public static final float FOV = 70f;

    private final Vector3f
            opos = new Vector3f(),
            pos = new Vector3f(),
            forwards = new Vector3f(0f, 0f, -1f),
            up = new Vector3f(0f, 1f, 0f),
            left = new Vector3f(1f, 0f, 0f);

    private float oxRot, oyRot, xRot, yRot;
    private final Quaternionf rotation = new Quaternionf();

    private final Matrix4f
            viewMatrix = new Matrix4f(),
            orthoMatrix = new Matrix4f(),
            perspMatrix = new Matrix4f();

    public void move(float x, float y, float z) {
        opos.set(pos);
        pos.add(new Vector3f(x, y, z).rotate(new Quaternionf().rotationY((float) Math.toRadians(-yRot))));
    }

    public void rotate(float yaw, float pitch) {
        this.oxRot = xRot;
        this.oyRot = yRot;
        this.xRot = pitch;
        this.yRot = yaw;
        this.rotation.rotationYXZ((float) Math.toRadians(-yaw), (float) Math.toRadians(-pitch), 0f);
        this.forwards.set(0f, 0f, -1f).rotate(this.rotation);
        this.up.set(0f, 1f, 0f).rotate(this.rotation);
        this.left.set(1f, 0f, 0f).rotate(this.rotation);
    }

    private void calculateMatrix(float d) {
        viewMatrix.identity();
        viewMatrix.rotate(Rotation.X.rotationDeg(Meth.lerp(oxRot, xRot, d)));
        viewMatrix.rotate(Rotation.Y.rotationDeg(Meth.lerp(oyRot, yRot, d)));
        //matrix.rotate(Rotation.Z.rotationDeg(180f));
        viewMatrix.translate(Meth.lerp(opos, pos, d).mul(-1));
    }

    public void updateProjMatrix(int width, int height) {
        perspMatrix.set(new Matrix4f().perspective((float) Math.toRadians(Camera.FOV), (float) width / height, 0.1f, 1000f));
        orthoMatrix.set(new Matrix4f().ortho(0, width, height, 0, 0, 1000));
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

    public Matrix4f getViewMatrix(float delta) {
        calculateMatrix(delta);
        return viewMatrix;
    }

    public Matrix4f getPerspectiveMatrix() {
        return perspMatrix;
    }

    public Matrix4f getOrthographicMatrix() {
        return orthoMatrix;
    }
}
