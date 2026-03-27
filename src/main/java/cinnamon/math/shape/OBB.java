package cinnamon.math.shape;

import org.joml.Math;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class OBB extends Shape {

    private final Vector3f center = new Vector3f(), halfExtents = new Vector3f();
    private final Quaternionf rotation = new Quaternionf();

    public OBB() {}

    public OBB(float centerX, float centerY, float centerZ, float halfX, float halfY, float halfZ) {
        this.set(centerX, centerY, centerZ, halfX, halfY, halfZ);
    }

    public OBB(float centerX, float centerY, float centerZ, float halfX, float halfY, float halfZ, Quaternionf rotation) {
        this.set(centerX, centerY, centerZ, halfX, halfY, halfZ, rotation);
    }

    public OBB(Vector3f center, Vector3f halfExtents) {
        this(center.x, center.y, center.z, halfExtents.x, halfExtents.y, halfExtents.z);
    }

    public OBB(Vector3f center, Vector3f halfExtents, Quaternionf rotation) {
        this(center.x, center.y, center.z, halfExtents.x, halfExtents.y, halfExtents.z, rotation);
    }

    public OBB(OBB obb) {
        this.center.set(obb.center);
        this.halfExtents.set(obb.halfExtents);
        this.rotation.set(obb.rotation);
    }

    public OBB set(Vector3f center, Vector3f halfExtents) {
        return this.set(center.x, center.y, center.z, halfExtents.x, halfExtents.y, halfExtents.z);
    }

    public OBB set(Vector3f center, Vector3f halfExtents, Quaternionf rotation) {
        return this.set(center.x, center.y, center.z, halfExtents.x, halfExtents.y, halfExtents.z, rotation);
    }

    public OBB set(float centerX, float centerY, float centerZ, float halfX, float halfY, float halfZ) {
        this.center.set(centerX, centerY, centerZ);
        this.halfExtents.set(halfX, halfY, halfZ);
        return this;
    }

    public OBB set(float centerX, float centerY, float centerZ, float halfX, float halfY, float halfZ, Quaternionf rotation) {
        this.center.set(centerX, centerY, centerZ);
        this.halfExtents.set(halfX, halfY, halfZ);
        this.rotation.set(rotation);
        return this;
    }

    public OBB setCenter(Vector3f center) {
        return this.setCenter(center.x, center.y, center.z);
    }

    public OBB setCenter(float x, float y, float z) {
        this.center.set(x, y, z);
        return this;
    }

    public Vector3f getCenter() {
        return center;
    }

    public OBB setHalfExtents(Vector3f halfExtents) {
        return this.setHalfExtents(halfExtents.x, halfExtents.y, halfExtents.z);
    }

    public OBB setHalfExtents(float x, float y, float z) {
        this.halfExtents.set(x, y, z);
        return this;
    }

    public Vector3f getHalfExtents() {
        return halfExtents;
    }

    public OBB setRotation(Quaternionf rotation) {
        this.rotation.set(rotation);
        return this;
    }

    public Quaternionf getRotation() {
        return rotation;
    }

    public OBB translate(Vector3f translation) {
        return this.translate(translation.x, translation.y, translation.z);
    }

    public OBB translate(float x, float y, float z) {
        this.center.add(x, y, z);
        return this;
    }

    public OBB scale(float scale) {
        return this.scale(scale, scale, scale);
    }

    public OBB scale(Vector3f scale) {
        return this.scale(scale.x, scale.y, scale.z);
    }

    public OBB scale(float scaleX, float scaleY, float scaleZ) {
        this.halfExtents.mul(scaleX, scaleY, scaleZ);
        return this;
    }

    public float getWidth() {
        return halfExtents.x * 2f;
    }

    public float getHeight() {
        return halfExtents.y * 2f;
    }

    public float getDepth() {
        return halfExtents.z * 2f;
    }

    public Vector3f getDimensions() {
        return new Vector3f(halfExtents).mul(2f);
    }

    public OBB rotate(Quaternionf rotation) {
        this.rotation.mul(rotation);
        return this;
    }

    public OBB rotateX(float angle) {
        this.rotation.rotateX(Math.toRadians(angle));
        return this;
    }

    public OBB rotateY(float angle) {
        this.rotation.rotateY(Math.toRadians(angle));
        return this;
    }

    public OBB rotateZ(float angle) {
        this.rotation.rotateZ(Math.toRadians(angle));
        return this;
    }

    public OBB applyMatrix(Matrix4f matrix) {
        int properties = matrix.properties();
        if ((properties & Matrix4f.PROPERTY_IDENTITY) != 0)
            return this;

        matrix.transformPosition(center);
        matrix.transformDirection(halfExtents);
        rotation.mul(matrix.getNormalizedRotation(new Quaternionf()));
        return this;
    }

    @Override
    public boolean containsPoint(float x, float y, float z) {
        return false;
    }

    @Override
    public float distanceToPoint(float x, float y, float z) {
        return -1f;
    }

    @Override
    public boolean intersectsAABB(AABB aabb) {
        return false;
    }

    @Override
    public boolean intersectsSphere(Sphere sphere) {
        return false;
    }

    @Override
    public boolean intersectsPlane(Plane plane) {
        return false;
    }

    @Override
    public boolean intersectsOBB(OBB obb) {
        return false;
    }
}

