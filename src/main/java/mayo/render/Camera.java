package mayo.render;

import mayo.utils.Rotation;
import mayo.world.entity.Entity;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;

public class Camera {

    public static final float FOV = 70f;

    private final Vector3f
            pos = new Vector3f(),
            forwards = new Vector3f(0f, 0f, -1f),
            up = new Vector3f(0f, 1f, 0f),
            left = new Vector3f(1f, 0f, 0f);

    private float xRot, yRot;
    private final Quaternionf rotation = new Quaternionf();

    private final Matrix4f
            viewMatrix = new Matrix4f(),
            orthoMatrix = new Matrix4f(),
            perspMatrix = new Matrix4f();

    private Entity entity;

    public void setup(Entity entity, boolean thirdPerson, float delta) {
        //rotation
        Vector2f rot = entity.getRot(delta);
        setRot(rot.x, rot.y);

        //position
        Vector3f pos = entity.getEyePos(delta);
        setPos(pos.x, pos.y, pos.z);

        //third person
        if (thirdPerson)
            move(0f, 0f, 3f);
    }

    protected void setPos(float x, float y, float z) {
        pos.set(x, y, z);
    }

    protected void setRot(float pitch, float yaw) {
        this.xRot = pitch;
        this.yRot = yaw;
        this.rotation.rotationYXZ((float) Math.toRadians(-yaw), (float) Math.toRadians(-pitch), 0f);
        this.forwards.set(0f, 0f, -1f).rotate(this.rotation);
        this.up.set(0f, 1f, 0f).rotate(this.rotation);
        this.left.set(1f, 0f, 0f).rotate(this.rotation);
    }

    protected void move(float x, float y, float z) {
        pos.add(new Vector3f(x, y, z).rotate(rotation));
    }

    public Matrix4f getViewMatrix() {
        viewMatrix.identity();
        viewMatrix.rotate(Rotation.X.rotationDeg(xRot));
        viewMatrix.rotate(Rotation.Y.rotationDeg(yRot));
        viewMatrix.translate(-pos.x, -pos.y, -pos.z);
        return viewMatrix;
    }

    public void updateProjMatrix(int width, int height) {
        perspMatrix.set(new Matrix4f().perspective((float) Math.toRadians(Camera.FOV), (float) width / height, 0.1f, 1000f));
        orthoMatrix.set(new Matrix4f().ortho(0, width, height, 0, -1000, 1000));
    }

    public void billboard(MatrixStack matrices) {
        verticalBillboard(matrices);
        horizontalBillboard(matrices);
    }

    public void horizontalBillboard(MatrixStack matrices) {
        matrices.rotate(Rotation.X.rotationDeg(xRot));
    }

    public void verticalBillboard(MatrixStack matrices) {
        matrices.rotate(Rotation.Y.rotationDeg(180f - yRot));
    }

    public boolean isInsideFrustum(float x, float y, float z) {
        Vector3f facing = getForwards();
        Vector3f target = new Vector3f(x, y, z).sub(pos).normalize();
        return facing.dot(target) > Math.cos(Math.toRadians(FOV));
    }

    public Vector3f getPos() {
        return new Vector3f(pos);
    }

    public Vector2f getRot() {
        return new Vector2f(xRot, yRot);
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

    public Matrix4f getPerspectiveMatrix() {
        return perspMatrix;
    }

    public Matrix4f getOrthographicMatrix() {
        return orthoMatrix;
    }
}
