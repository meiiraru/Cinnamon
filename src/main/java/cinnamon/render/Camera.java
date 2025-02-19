package cinnamon.render;

import cinnamon.utils.AABB;
import cinnamon.utils.Maths;
import cinnamon.utils.Rotation;
import cinnamon.world.collisions.Hit;
import cinnamon.world.entity.Entity;
import org.joml.*;

import java.lang.Math;

import static org.lwjgl.opengl.GL11.glColorMask;

public class Camera {

    public static final float
            NEAR_PLANE = 0.1f,
            FAR_PLANE = 1000f;

    private final Frustum frustum = new Frustum();

    private final Vector3f
            pos = new Vector3f(),
            forwards = new Vector3f(0f, 0f, -1f),
            up = new Vector3f(0f, 1f, 0f),
            left = new Vector3f(1f, 0f, 0f);

    private final Vector2f rot = new Vector2f();
    private final Quaternionf rotation = new Quaternionf();

    private final Matrix4f
            viewMatrix = new Matrix4f(),
            orthoMatrix = new Matrix4f(),
            perspMatrix = new Matrix4f();
    private boolean isOrtho = true;

    private Entity entity;

    public void setup(int mode, float delta) {
        if (this.entity == null)
            return;

        //rotation
        Vector2f rot = entity.getRot(delta);
        setRot(rot.x, rot.y);

        //position
        Vector3f pos = entity.getEyePos(delta);
        setPos(pos.x, pos.y, pos.z);

        //third person
        if (mode == 1)
            move(0f, 0f, Math.max(entity.getEyeHeight(), 1f) * 3f);
        //third person front
        else if (mode == 2) {
            setRot(-rot.x, rot.y + 180);
            move(0f, 0f, Math.max(entity.getEyeHeight(), 1f) * 3f);
        }
    }

    public void setPos(float x, float y, float z) {
        pos.set(x, y, z);
    }

    public void setRot(float pitch, float yaw) {
        this.rot.set(pitch, yaw);
        this.rotation.rotationYXZ((float) Math.toRadians(-yaw), (float) Math.toRadians(-pitch), 0f);
        this.forwards.set(0f, 0f, -1f).rotate(this.rotation);
        this.up.set(0f, 1f, 0f).rotate(this.rotation);
        this.left.set(1f, 0f, 0f).rotate(this.rotation);
    }

    public void move(float x, float y, float z) {
        float epsilon = 1 - NEAR_PLANE;
        Vector3f move = new Vector3f(x, y, z).mul(1f / epsilon).rotate(rotation);
        AABB area = new AABB();
        area.translate(pos);
        area.expand(move);

        if (entity != null) {
            Hit<?> hit = entity.getWorld().raycastTerrain(area, pos, move);
            if (hit != null)
                move.mul(hit.collision().near());
        }

        pos.add(move.mul(epsilon));
    }

    public Matrix4f getViewMatrix() {
        viewMatrix.identity();

        if (isOrtho)
            return viewMatrix;

        viewMatrix.rotate(Rotation.X.rotationDeg(rot.x));
        viewMatrix.rotate(Rotation.Y.rotationDeg(rot.y));
        viewMatrix.translate(-pos.x, -pos.y, -pos.z);

        return viewMatrix;
    }

    public void updateProjMatrix(int width, int height, float fov) {
        perspMatrix.set(new Matrix4f().perspective((float) Math.toRadians(fov), (float) width / height, NEAR_PLANE, FAR_PLANE));
        orthoMatrix.set(new Matrix4f().ortho(0, width, height, 0, -1000, 1000));
    }

    public void billboard(MatrixStack matrices) {
        horizontalBillboard(matrices);
        verticalBillboard(matrices);
    }

    public void horizontalBillboard(MatrixStack matrices) {
        matrices.rotate(Rotation.Y.rotationDeg(180f - rot.y));
    }

    public void verticalBillboard(MatrixStack matrices) {
        matrices.rotate(Rotation.X.rotationDeg(rot.x));
    }

    public boolean isInsideFrustum(AABB aabb) {
        return !frustum.culledXY(aabb.minX(), aabb.minY(), aabb.minZ(), aabb.maxX(), aabb.maxY(), aabb.maxZ());
    }

    public boolean isInsideFrustum(float x, float y, float z) {
        return !frustum.culledXY(x, y, z);
    }

    public Vector4f worldToScreenSpace(float x, float y, float z) {
        Matrix3f transformMatrix = new Matrix3f().rotation(rotation);
        transformMatrix.invert();

        Vector3f posDiff = new Vector3f(x, y, z).sub(pos);
        transformMatrix.transform(posDiff);

        Vector4f projectiveCamSpace = new Vector4f(posDiff, 1f);
        perspMatrix.transform(projectiveCamSpace);

        float w = projectiveCamSpace.w();
        return new Vector4f(projectiveCamSpace.x() / w, projectiveCamSpace.y() / w, projectiveCamSpace.z() / w, (float) Math.sqrt(posDiff.dot(posDiff)));
    }

    public void lookAt(float x, float y, float z) {
        Vector3f direction = new Vector3f(x, y, z).sub(pos).normalize();
        Vector2f rot = Maths.dirToRot(direction);
        setRot(rot.x, rot.y);
    }

    public void updateFrustum() {
        Matrix4f proj = getProjectionMatrix();
        Matrix4f mvp = getViewMatrix().mulLocal(proj);
        frustum.updateFrustum(mvp);
    }

    public void updateFrustum(Matrix4f mvp) {
        frustum.updateFrustum(mvp);
    }

    public void anaglyph3D(MatrixStack matrices, float eyeDistance, float angle, Runnable customBufferRenderer, Runnable mainBufferRenderer) {
        boolean left = angle < 0;
        float angleRad = (float) Math.toRadians(angle);

        for (int i = 0; i < 2; i++) {
            //move and rotate matrices to eye position
            Quaternionf rot = new Quaternionf().setAngleAxis(left ? -angleRad : angleRad, up.x, up.y, up.z);
            Vector3f offset = new Vector3f(left ? -eyeDistance : eyeDistance, 0, 0).rotate(rotation);

            matrices.push();
            matrices.translate(pos.x + offset.x, pos.y + offset.y, pos.z + offset.z);
            matrices.rotate(rot);
            matrices.translate(-pos.x, -pos.y, -pos.z);

            //custom renderer
            if (customBufferRenderer != null)
                customBufferRenderer.run();

            //apply eye color mask
            glColorMask(left, !left, !left, true);

            //finish rendering
            mainBufferRenderer.run();

            //cleanup
            matrices.pop();
            glColorMask(true, true, true, true);
            left = !left;
        }
    }

    public void setEntity(Entity entity) {
        this.entity = entity;
    }

    public void reset() {
        setPos(0f, 0f, 0f);
        setRot(0f, 0f);
    }

    public void useOrtho(boolean ortho) {
        isOrtho = ortho;
    }

    public Vector3f getPos() {
        return pos;
    }

    public Vector2f getRot() {
        return rot;
    }

    public Vector3f getForwards() {
        return forwards;
    }

    public Vector3f getLeft() {
        return left;
    }

    public Vector3f getUp() {
        return up;
    }

    public Quaternionf getRotation() {
        return rotation;
    }

    public Matrix4f getProjectionMatrix() {
        return isOrtho ? orthoMatrix : perspMatrix;
    }

    public Entity getEntity() {
        return entity;
    }
}
