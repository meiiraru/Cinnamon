package cinnamon.math.shape;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Sphere extends Shape {

    private float x, y, z;
    private float radius;

    public Sphere() {
        this(0, 0, 0, 0);
    }

    public Sphere(Vector3f center, float radius) {
        this(center.x, center.y, center.z, radius);
    }

    public Sphere(float x, float y, float z, float radius) {
        this.set(x, y, z, radius);
    }

    public Sphere(Sphere sphere) {
        this.set(sphere.x, sphere.y, sphere.z, sphere.radius);
    }

    public Sphere set(Sphere sphere) {
        return this.set(sphere.x, sphere.y, sphere.z, sphere.radius);
    }

    public Sphere set(Vector3f center, float radius) {
        return this.set(center.x, center.y, center.z, radius);
    }

    public Sphere set(float x, float y, float z, float radius) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.radius = radius;
        return this;
    }

    public Sphere setCenter(Vector3f center) {
        return this.setCenter(center.x, center.y, center.z);
    }

    public Sphere setCenter(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    public Sphere setRadius(float radius) {
        this.radius = radius;
        return this;
    }

    public Vector3f getCenter() {
        return new Vector3f(x, y, z);
    }

    public float getRadius() {
        return radius;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getZ() {
        return z;
    }

    public Sphere translate(Vector3f translation) {
        return this.translate(translation.x, translation.y, translation.z);
    }

    public Sphere translate(float x, float y, float z) {
        this.x += x;
        this.y += y;
        this.z += z;
        return this;
    }

    public Sphere scale(float scale) {
        this.radius *= scale;
        return this;
    }

    public Sphere applyMatrix(Matrix4f matrix) {
        Vector3f center = this.getCenter().mulPosition(matrix);
        this.setCenter(center);

        float scaleX = Vector3f.length(matrix.m00(), matrix.m10(), matrix.m20());
        float scaleY = Vector3f.length(matrix.m01(), matrix.m11(), matrix.m21());
        float scaleZ = Vector3f.length(matrix.m02(), matrix.m12(), matrix.m22());
        float maxScale = Math.max(scaleX, Math.max(scaleY, scaleZ));
        this.scale(maxScale);

        return this;
    }

    @Override
    public boolean containsPoint(float x, float y, float z) {
        return Vector3f.distanceSquared(this.x, this.y, this.z, x, y, z) <= radius * radius;
    }

    @Override
    public float distanceToPoint(float x, float y, float z) {
        float centerToPointDist = Vector3f.distance(this.x, this.y, this.z, x, y, z);
        return Math.max(0f, centerToPointDist - radius);
    }

    @Override
    public boolean intersectsAABB(AABB aabb) {
        return aabb.intersectsSphere(this);
    }

    @Override
    public boolean intersectsSphere(Sphere other) {
        float radiusSum = this.radius + other.radius;
        return Vector3f.distanceSquared(this.x, this.y, this.z, other.x, other.y, other.z) <= radiusSum * radiusSum;
    }

    @Override
    public boolean intersectsPlane(Plane plane) {
        float distanceToPlane = plane.getNormal().dot(x, y, z) + plane.getConstant();
        return Math.abs(distanceToPlane) <= radius;
    }

    @Override
    public boolean intersectsOBB(OBB obb) {
        return obb.intersectsSphere(this);
    }
}
