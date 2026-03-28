package cinnamon.math.shape;

import org.joml.Vector3f;

public abstract class Shape {

    public boolean containsPoint(Vector3f point) {
        return this.containsPoint(point.x, point.y, point.z);
    }
    public abstract boolean containsPoint(float x, float y, float z);

    public float distanceToPoint(Vector3f point) {
        return this.distanceToPoint(point.x, point.y, point.z);
    }
    public abstract float distanceToPoint(float x, float y, float z);

    public abstract boolean intersectsAABB(AABB aabb);
    public abstract boolean intersectsSphere(Sphere sphere);
    public abstract boolean intersectsPlane(Plane plane);
    public abstract boolean intersectsOBB(OBB obb);

    public abstract Ray.Hit collideRay(Ray ray);
}
