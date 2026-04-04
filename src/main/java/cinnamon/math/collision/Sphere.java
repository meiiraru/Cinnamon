package cinnamon.math.collision;

import cinnamon.math.Maths;
import org.joml.Math;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Sphere extends Collider<Sphere> {

    private final Vector3f center = new Vector3f();
    private float radius;

    public Sphere() {
        this(0, 0, 0, 0);
    }

    public Sphere(float radius) {
        this(0, 0, 0, radius);
    }

    public Sphere(Vector3f center, float radius) {
        this(center.x, center.y, center.z, radius);
    }

    public Sphere(float x, float y, float z, float radius) {
        this.set(x, y, z, radius);
    }

    public Sphere(Sphere sphere) {
        this.set(sphere);
    }

    public Sphere(AABB aabb) {
        this.set(aabb);
    }

    public Sphere(OBB obb) {
        this.set(obb);
    }

    public Sphere set(Sphere sphere) {
        return this.set(sphere.center, sphere.radius);
    }

    public Sphere set(AABB aabb) {
        Vector3f center = aabb.getCenter();
        float radius = aabb.getDimensions().length() * 0.5f;
        return this.set(center, radius);
    }

    public Sphere set(OBB obb) {
        Vector3f center = obb.getCenter();
        float radius = obb.getHalfExtents().length();
        return this.set(center, radius);
    }

    public Sphere set(Vector3f center, float radius) {
        return this.set(center.x, center.y, center.z, radius);
    }

    public Sphere set(float x, float y, float z, float radius) {
        this.setCenter(x, y, z);
        this.setRadius(radius);
        return this;
    }

    @Override
    public Sphere clone() {
        return new Sphere(this);
    }

    @Override
    public Sphere setCenter(float x, float y, float z) {
        this.center.set(x, y, z);
        return this;
    }

    public Sphere setRadius(float radius) {
        this.radius = radius;
        return this;
    }

    @Override
    public Vector3f getCenter() {
        return center;
    }

    public float getRadius() {
        return radius;
    }

    @Override
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
    public Vector3f closestPoint(float x, float y, float z, Vector3f out) {
        float dx = x - center.x;
        float dy = y - center.y;
        float dz = z - center.z;

        float distSqr = Vector3f.lengthSquared(dx, dy, dz);
        if (distSqr <= radius * radius)
            return out.set(x, y, z);

        float scale = radius / Math.sqrt(distSqr);
        return out.set(center.x + dx * scale, center.y + dy * scale, center.z + dz * scale);
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

        //normalize the intersection times
        float normNear = tNear / maxDist;
        float normFar  = tFar  / maxDist;

        //calculate raycast result
        Vector3f hitPos = origin.fma(tNear, dir, new Vector3f());

        //(hitPoint - center) / radius
        Vector3f hitNormal = new Vector3f((hitPos.x - center.x) / radius, (hitPos.y - center.y) / radius, (hitPos.z - center.z) / radius);
        return new Hit(hitPos, hitNormal, normNear, normFar, ray, this);
    }

    @Override
    public void project(Vector3f axis, float[] minMax) {
        float centerProj = center.dot(axis);
        minMax[0] = centerProj - radius;
        minMax[1] = centerProj + radius;
    }

    @Override
    public Collision collideAABB(AABB aabb) {
        Collision col = aabb.collideSphere(this);
        return col != null ? col.invert() : null;
    }

    @Override
    public Collision collideOBB(OBB obb) {
        Collision col = obb.collideSphere(this);
        return col != null ? col.invert() : null;
    }

    @Override
    public Collision collideSphere(Sphere sphere) {
        float dx = sphere.center.x - this.center.x;
        float dy = sphere.center.y - this.center.y;
        float dz = sphere.center.z - this.center.z;

        float distSq = dx * dx + dy * dy + dz * dz;
        float radiusSum = this.radius + sphere.radius;

        if (distSq > radiusSum * radiusSum)
            return null;

        float dist = Math.sqrt(distSq);
        float depth = radiusSum - dist;
        Vector3f normal = (dist > Maths.KINDA_SMALL_NUMBER) ? new Vector3f(dx / dist, dy / dist, dz / dist) : new Vector3f(1, 0, 0);

        return new Collision(normal, depth, this, sphere);
    }

    @Override
    public Hit sweepSphere(Sphere sphere, Vector3f velocity) {
        return null;
    }

    @Override
    public Hit sweepAABB(AABB aabb, Vector3f velocity) {
        return null;
    }

    @Override
    public Hit sweepOBB(OBB obb, Vector3f velocity) {
        return null;
    }

    @Override
    public String toString() {
        return "Sphere{x=" + center.x + " y=" + center.y + " z=" + center.z + " r=" + radius + "}";
    }
}
