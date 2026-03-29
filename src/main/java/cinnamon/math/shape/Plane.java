package cinnamon.math.shape;

import cinnamon.math.Maths;
import org.joml.Math;
import org.joml.Vector3f;

public class Plane extends Shape {

    private final Vector3f normal = new Vector3f();
    private float constant;

    public Plane() {
        this(0, 0, 0, 0);
    }

    public Plane(Vector3f normal, float constant) {
        this(normal.x, normal.y, normal.z, constant);
    }

    public Plane(float normalX, float normalY, float normalZ, float constant) {
        this.set(normalX, normalY, normalZ, constant);
    }

    public Plane(Plane plane) {
        this.set(plane.normal, plane.constant);
    }

    public Plane set(Plane plane) {
        return this.set(plane.normal, plane.constant);
    }

    public Plane set(Vector3f normal, Vector3f point) {
        return this.set(normal, -normal.dot(point));
    }

    public Plane set(Vector3f normal, float constant) {
        return this.set(normal.x, normal.y, normal.z, constant);
    }

    public Plane set(float normalX, float normalY, float normalZ, float constant) {
        this.setNormal(normalX, normalY, normalZ);
        this.setConstant(constant);
        return this;
    }

    public Plane set(Vector3f p1, Vector3f p2, Vector3f p3) {
        this.normal.set(p2.x - p1.x, p2.y - p1.y, p2.z - p1.z).cross(p3.x - p1.x, p3.y - p1.y, p3.z - p1.z).normalize();
        this.constant = -normal.dot(p1);
        return this;
    }

    public Plane normalize() {
        float length = normal.length();
        if (length > 0) {
            float invLength = 1f / length;
            normal.mul(invLength);
            constant *= invLength;
        }
        return this;
    }

    public Plane negate() {
        normal.negate();
        constant = -constant;
        return this;
    }

    public Vector3f setNormal(Vector3f normal) {
        return this.setNormal(normal.x, normal.y, normal.z);
    }

    public Vector3f setNormal(float x, float y, float z) {
        return this.normal.set(x, y, z);
    }

    public Vector3f getNormal() {
        return normal;
    }

    public void setConstant(float constant) {
        this.constant = constant;
    }

    public float getConstant() {
        return constant;
    }

    public Vector3f projectPoint(Vector3f point) {
        float distanceToPoint = normal.dot(point) + constant;
        return new Vector3f(point).sub(normal.x * distanceToPoint, normal.y * distanceToPoint, normal.z * distanceToPoint);
    }

    @Override
    public boolean containsPoint(float x, float y, float z) {
        return Math.abs(normal.dot(x, y, z) + constant) < Maths.KINDA_SMALL_NUMBER;
    }

    @Override
    public float distanceToPoint(float x, float y, float z) {
        return normal.dot(x, y, z) + constant;
    }

    @Override
    public boolean intersectsAABB(AABB aabb) {
        return aabb.intersectsPlane(this);
    }

    @Override
    public boolean intersectsSphere(Sphere sphere) {
        return sphere.intersectsPlane(this);
    }

    @Override
    public boolean intersectsPlane(Plane plane) {
        Vector3f cross = new Vector3f(normal).cross(plane.normal);
        return cross.lengthSquared() > Maths.KINDA_SMALL_NUMBER;
    }

    @Override
    public boolean intersectsOBB(OBB obb) {
        return obb.intersectsPlane(this);
    }

    @Override
    public Ray.Hit collideRay(Ray ray) {
        //find the angle between the ray direction and the plane normal
        Vector3f dir = ray.getDirection();
        float dot = normal.dot(dir);
        if (Math.abs(dot) < Maths.KINDA_SMALL_NUMBER)
            return null;

        //find the distance along the ray to the intersection point
        Vector3f origin = ray.getOrigin();
        float t = -(normal.dot(origin) + constant) / dot;
        float maxDist = ray.getMaxDistance();
        if (t < 0 || t > maxDist)
            return null;

        //calculate raycast result
        Vector3f hitPos = origin.fma(t, dir, new Vector3f());
        Vector3f hitNormal = new Vector3f(normal);
        if (dot > 0) hitNormal.negate();

        return new Ray.Hit(hitPos, hitNormal, t, t, maxDist, this);
    }

    @Override
    public String toString() {
        return "Plane{nx=" + normal.x + " ny=" + normal.y + " nz=" + normal.z + " d=" + constant + "}";
    }
}
