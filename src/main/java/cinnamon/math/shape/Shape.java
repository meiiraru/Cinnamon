package cinnamon.math.shape;

import org.joml.Vector3f;

public abstract class Shape {

    public boolean containsPoint(Vector3f point) {
        return this.containsPoint(point.x, point.y, point.z);
    }

    public abstract boolean containsPoint(float x, float y, float z);

    public abstract boolean intersectsAABB(AABB aabb);
    public abstract boolean intersectsSphere(Sphere sphere);
    public abstract boolean intersectsPlane(Plane plane);
}
