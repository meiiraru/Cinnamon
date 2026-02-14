package cinnamon.render;

import cinnamon.utils.AABB;
import cinnamon.utils.Maths;
import cinnamon.utils.Rotation;
import cinnamon.vr.XrManager;
import cinnamon.world.collisions.Hit;
import cinnamon.world.entity.Entity;
import cinnamon.world.entity.PhysEntity;
import org.joml.*;
import org.joml.Math;

import static org.lwjgl.opengl.GL11.glColorMask;

public class Camera {

    public static float
            NEAR_PLANE = 0.05f,
            FAR_PLANE = 1000f;

    private float fov = 90f;
    private float aspectRatio = 16f / 9f;
    private int width = 640, height = 360;

    private final Frustum frustum = new Frustum();

    private final Vector3f
            pos = new Vector3f(),
            xrPos = new Vector3f();

    private final Quaternionf
            rotation = new Quaternionf(),
            xrRot = new Quaternionf();

    private final Matrix3f
            dirMatrix = new Matrix3f();

    private final Matrix4f
            identity = new Matrix4f(),
            viewMatrix = new Matrix4f(),
            orthoMatrix = new Matrix4f(),
            perspMatrix = new Matrix4f(),
            invViewMatrix = new Matrix4f(),
            invOrthoMatrix = new Matrix4f(),
            invPerspMatrix = new Matrix4f();

    private boolean
            isOrtho = true,
            viewDirty = true;

    private Entity entity;

    public void setup(int mode, float delta) {
        if (this.entity == null)
            return;

        //rotation
        Vector2f rot = entity.getRot(delta);
        setRot(rot.x, rot.y, 0f);

        //position
        Vector3f pos = entity.getEyePos(delta);
        setPos(pos.x, pos.y, pos.z);

        //third person
        if (mode == 1)
            move(0f, 0f, Math.max(entity.getEyeHeight(), 1f) * 4f);
        //third person front
        else if (mode == 2) {
            setRot(-rot.x, rot.y + 180, 0f);
            move(0f, 0f, Math.max(entity.getEyeHeight(), 1f) * 4f);
        }
    }

    public void setPos(float x, float y, float z) {
        pos.set(x, y, z);
        viewDirty = true;
    }

    public void setRot(float pitch, float yaw, float roll) {
        this.rotation.rotationZYX(Math.toRadians(roll), Math.toRadians(-yaw), Math.toRadians(-pitch));
        viewDirty = true;
    }

    public void setRot(Quaternionf quaternion) {
        this.rotation.set(quaternion);
        viewDirty = true;
    }

    public void move(float x, float y, float z) {
        Vector3f move = new Vector3f(x, y, z).rotate(rotation);
        float dist = getClipDistance(move);
        move.normalize().mul(dist);
        setPos(pos.x + move.x, pos.y + move.y, pos.z + move.z);
    }

    public float getClipDistance(Vector3f direction) {
        float distance = direction.length();
        if (entity == null || distance <= 0)
            return distance;

        //calculate frustum dimensions at near plane
        float s = NEAR_PLANE * 2f;

        Vector3f rayStart = new Vector3f();
        Vector3f offset = new Vector3f();
        AABB area = new AABB();
        float maxDist = distance;

        //cast 8 rays from the camera corner offsets
        for (int i = 0; i < 8; i++) {
            float ox = ((i & 1) * 2 - 1) * s;
            float oy = ((i >> 1 & 1) * 2 - 1) * s;
            float oz = ((i >> 2 & 1) * 2 - 1) * s;

            //rotate offset to camera space
            offset.set(ox, oy, oz).rotate(rotation);
            rayStart.set(pos.x + offset.x, pos.y + offset.y, pos.z + offset.z);
            area.set(rayStart).expand(direction);

            Hit<?> hit = entity.getWorld().raycastTerrain(area, rayStart, direction, t ->
                    t.isSelectable(entity) && (!(entity instanceof PhysEntity pe) || pe.getTerrainCollisionMask().test(t.getCollisionMask()))
            );

            if (hit != null) {
                float hitDist = hit.collision().near() * distance;
                if (hitDist < maxDist)
                    maxDist = hitDist;
            }
        }

        return maxDist;
    }

    public void updateProjMatrix(int width, int height, float fov) {
        this.fov = fov;
        this.aspectRatio = (float) width / height;
        this.width = width;
        this.height = height;
        resetProjMatrix();
    }

    public void resetProjMatrix() {
        perspMatrix.identity().perspective(Math.toRadians(fov), aspectRatio, NEAR_PLANE, FAR_PLANE);
        orthoMatrix.identity().ortho(0, width, height, 0, -1000, 1000);
        invPerspMatrix.identity().set(perspMatrix).invert();
        invOrthoMatrix.identity().set(orthoMatrix).invert();
    }

    public void setXrTransform(float x, float y, float z, float qx, float qy, float qz, float qw) {
        xrPos.set(x, y, z);
        xrRot.set(qx, qy, qz, qw);
        reset();
    }

    protected void recalculateViewMatrix() {
        if (!viewDirty)
            return;

        Quaternionf rotation = getRotation();
        viewMatrix.translationRotateScaleInvert(getPosition(), rotation, 1f);
        invViewMatrix.set(viewMatrix).invert();
        dirMatrix.rotation(rotation.invert(new Quaternionf()));

        viewDirty = false;
    }

    /**
     * Applies a billboard rotation to the given matrices
     * @param matrices the matrix stack to apply the rotation
     */
    public void billboard(MatrixStack matrices) {
        billboard(matrices, (byte) 0x7);
    }

    /**
     * Applies an axis-based billboard rotation to the given matrices
     * @param matrices the matrix stack to apply the rotation
     * @param rotationMask 0x1 = Pitch (X), 0x2 = Yaw (Y), 0x4 = Roll (Z)
     */
    public void billboard(MatrixStack matrices, byte rotationMask) {
        Quaternionf rot = getRotation();
        if ((rotationMask & 0x2) != 0)
            matrices.rotate(Rotation.Y.rotationDeg(-Maths.getYaw(rot) + 180f));
        if ((rotationMask & 0x4) != 0)
            matrices.rotate(Rotation.Z.rotationDeg(Maths.getRoll(rot)));
        if ((rotationMask & 0x1) != 0)
            matrices.rotate(Rotation.X.rotationDeg(Maths.getPitch(rot)));
    }

    public boolean isInsideFrustum(AABB aabb) {
        return frustum.isBoxInside(aabb.minX(), aabb.minY(), aabb.minZ(), aabb.maxX(), aabb.maxY(), aabb.maxZ());
    }

    public boolean isInsideFrustum(float x, float y, float z) {
        return frustum.isPointInside(x, y, z);
    }

    public Vector4f worldToScreenSpace(float x, float y, float z) {
        Matrix3f transformMatrix = new Matrix3f().rotation(getRotation());
        transformMatrix.invert();

        Vector3f posDiff = new Vector3f(x, y, z).sub(getPosition());
        transformMatrix.transform(posDiff);

        Vector4f projectiveCamSpace = new Vector4f(posDiff, 1f);
        perspMatrix.transform(projectiveCamSpace);

        float w = projectiveCamSpace.w();
        return new Vector4f(projectiveCamSpace.x() / w, projectiveCamSpace.y() / w, projectiveCamSpace.z() / w, Math.sqrt(posDiff.dot(posDiff)));
    }

    public void lookAt(float x, float y, float z) {
        Vector3f direction = new Vector3f(x, y, z).sub(getPosition()).normalize();
        Vector2f rot = Maths.dirToRot(direction);
        setRot(rot.x, rot.y, 0f);
    }

    public void updateFrustum() {
        frustum.update(getProjectionMatrix(), getViewMatrix());
    }

    public void updateFrustum(Matrix4f viewProj) {
        frustum.update(viewProj);
    }

    public void anaglyph3D(MatrixStack matrices, float eyeDistance, float angle, Runnable beforeColor, Runnable targetRender) {
        boolean left = angle < 0;
        float angleRad = Math.toRadians(angle);

        for (int i = 0; i < 2; i++) {
            //move and rotate matrices to eye position
            Vector3f up = getUp();
            Quaternionf rotation = getRotation();
            Quaternionf rot = new Quaternionf().setAngleAxis(left ? -angleRad : angleRad, up.x, up.y, up.z);
            Vector3f offset = new Vector3f(left ? -eyeDistance : eyeDistance, 0, 0).rotate(rotation);

            matrices.pushMatrix();
            Vector3f pos = getPosition();
            matrices.translate(pos.x + offset.x, pos.y + offset.y, pos.z + offset.z);
            matrices.rotate(rot);
            matrices.translate(-pos.x, -pos.y, -pos.z);

            //renderer
            if (beforeColor != null)
                beforeColor.run();

            //apply eye color mask
            glColorMask(left, !left, !left, true);

            //finish rendering
            targetRender.run();

            //cleanup
            matrices.popMatrix();
            glColorMask(true, true, true, true);
            left = !left;
        }
    }

    public void setEntity(Entity entity) {
        this.entity = entity;
    }

    public void reset() {
        setPos(0f, 0f, 0f);
        setRot(0f, 0f, 0f);
    }

    public void useOrtho(boolean ortho) {
        isOrtho = ortho;
    }

    public Vector3f getPos() {
        return pos;
    }

    public Vector3f getXrPos() {
        return xrPos;
    }

    public Quaternionf getRot() {
        return rotation;
    }

    public Quaternionf getXrRot() {
        return xrRot;
    }

    public Vector3f getForwards() {
        recalculateViewMatrix();
        return dirMatrix.normalizedPositiveZ(new Vector3f()).negate();
    }

    public Vector3f getLeft() {
        recalculateViewMatrix();
        return dirMatrix.normalizedPositiveX(new Vector3f()).negate();
    }

    public Vector3f getUp() {
        recalculateViewMatrix();
        return dirMatrix.normalizedPositiveY(new Vector3f());
    }

    public Matrix3f getDirectionMatrix() {
        recalculateViewMatrix();
        return dirMatrix;
    }

    public Vector3f getPosition() {
        if (!XrManager.isInXR())
            return pos;

        if (isActuallyInOrtho())
            return xrPos;

        return xrPos.rotate(rotation, new Vector3f()).add(pos);
    }

    public Quaternionf getRotation() {
        if (!XrManager.isInXR())
            return rotation;

        if (isActuallyInOrtho())
            return xrRot;

        return rotation.mul(xrRot, new Quaternionf());
    }

    public Matrix4f getProjectionMatrix() {
        return isOrtho() ? orthoMatrix : perspMatrix;
    }

    public Matrix4f getViewMatrix() {
        recalculateViewMatrix();
        return isOrtho() ? identity : viewMatrix;
    }

    public Matrix4f getInvProjectionMatrix() {
        return isOrtho() ? invOrthoMatrix : invPerspMatrix;
    }

    public Matrix4f getInvViewMatrix() {
        recalculateViewMatrix();
        return isOrtho() ? identity : invViewMatrix;
    }

    public boolean isOrtho() {
        return isOrtho && !XrManager.isInXR();
    }

    private boolean isActuallyInOrtho() {
        return isOrtho;
    }

    public Entity getEntity() {
        return entity;
    }

    public Frustum getFrustum() {
        return frustum;
    }

    public float getFov() {
        return fov;
    }

    public float getAspectRatio() {
        return aspectRatio;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void copyFrom(Camera camera, boolean includeView) {
        //copy properties
        this.fov = camera.fov;
        this.aspectRatio = camera.aspectRatio;
        this.width = camera.width;
        this.height = camera.height;

        //copy projection matrices
        this.perspMatrix.set(camera.perspMatrix);
        this.orthoMatrix.set(camera.orthoMatrix);
        this.invPerspMatrix.set(camera.invPerspMatrix);
        this.invOrthoMatrix.set(camera.invOrthoMatrix);
        this.isOrtho = camera.isOrtho;

        //copy view properties
        if (includeView) {
            this.pos.set(camera.pos);
            this.xrPos.set(camera.xrPos);
            this.rotation.set(camera.rotation);
            this.xrRot.set(camera.xrRot);
            this.entity = camera.entity;
            this.viewDirty = true;
        }
    }
}
