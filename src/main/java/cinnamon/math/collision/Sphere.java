package cinnamon.math.collision;

import org.joml.Math;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Sphere extends CollisionShape {

    private final Vector3f center = new Vector3f();
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
        this.set(sphere.center, sphere.radius);
    }

    public Sphere set(Sphere sphere) {
        return this.set(sphere.center, sphere.radius);
    }

    public Sphere set(Vector3f center, float radius) {
        return this.set(center.x, center.y, center.z, radius);
    }

    public Sphere set(float x, float y, float z, float radius) {
        this.setCenter(x, y, z);
        this.setRadius(radius);
        return this;
    }

    public Sphere setCenter(Vector3f center) {
        return this.setCenter(center.x, center.y, center.z);
    }

    public Sphere setCenter(float x, float y, float z) {
        this.center.set(x, y, z);
        return this;
    }

    public Sphere setRadius(float radius) {
        this.radius = radius;
        return this;
    }

    public Vector3f getCenter() {
        return center;
    }

    public float getRadius() {
        return radius;
    }

    public Sphere translate(Vector3f translation) {
        return this.translate(translation.x, translation.y, translation.z);
    }

    public Sphere translate(float x, float y, float z) {
        this.center.add(x, y, z);
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
        return this.center.distanceSquared(x, y, z) <= radius * radius;
    }

    @Override
    public float distanceToPoint(float x, float y, float z) {
        float centerToPointDist = this.center.distance(x, y, z);
        return Math.max(0f, centerToPointDist - radius);
    }

    @Override
    public boolean intersectsPlane(Plane plane) {
        float distanceToPlane = plane.getNormal().dot(center) + plane.getConstant();
        return Math.abs(distanceToPlane) <= radius;
    }

    @Override
    public boolean intersectsSphere(Sphere other) {
        float radiusSum = this.radius + other.radius;
        return this.center.distanceSquared(other.center) <= radiusSum * radiusSum;
    }

    @Override
    public boolean intersectsAABB(AABB aabb) {
        return aabb.intersectsSphere(this);
    }

    @Override
    public boolean intersectsOBB(OBB obb) {
        return obb.intersectsSphere(this);
    }

    @Override
    public Hit collideRay(Ray ray) {
        Vector3f dir = ray.getDirection();
        Vector3f origin = ray.getOrigin();

        //sphere center to ray origin
        float ocX = origin.x - center.x;
        float ocY = origin.y - center.y;
        float ocZ = origin.z - center.z;

        //standard quadratic formula for ray-sphere intersection
        //float a = dir.dot(dir); // always 1
        float b = dir.dot(ocX, ocY, ocZ);
        float c = (ocX * ocX + ocY * ocY + ocZ * ocZ) - radius * radius;
        float discriminant = b * b - c;
        if (discriminant < 0)
            return null;

        float sqrtD = Math.sqrt(discriminant);
        float tNear = -b - sqrtD;
        float tFar = -b + sqrtD;
        float maxDist = ray.getMaxDistance();

        //check if the sphere is entirely behind the ray or beyond max distance
        if (tFar < 0 || tNear > maxDist)
            return null;

        //calculate raycast result
        float tHit = Math.max(0, tNear);
        Vector3f hitPos = origin.fma(tHit, dir, new Vector3f());

        //(hitPoint - center) / radius
        Vector3f hitNormal = new Vector3f();
        if (tNear > 0f)
            hitNormal.set((hitPos.x - center.x) / radius, (hitPos.y - center.y) / radius, (hitPos.z - center.z) / radius);
        else
            hitNormal.set(-dir.x, -dir.y, -dir.z);

        return new Hit(hitPos, hitNormal, tHit, tFar, ray, this);
    }

    @Override
    public String toString() {
        return "Sphere{x=" + center.x + " y=" + center.y + " z=" + center.z + " r=" + radius + "}";
    }
}
