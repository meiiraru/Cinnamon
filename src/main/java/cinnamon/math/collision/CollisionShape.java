package cinnamon.math.collision;

import org.joml.Vector3f;

public abstract class CollisionShape<T extends CollisionShape<T>> {

    public abstract Vector3f getCenter();

    public T translate(Vector3f translation) {
        return this.translate(translation.x, translation.y, translation.z);
    }

    public abstract T translate(float x, float y, float z);

    public boolean containsPoint(Vector3f point) {
        return this.containsPoint(point.x, point.y, point.z);
    }
    public abstract boolean containsPoint(float x, float y, float z);

    public float distanceToPoint(Vector3f point) {
        return this.distanceToPoint(point.x, point.y, point.z);
    }
    public abstract float distanceToPoint(float x, float y, float z);

    public boolean intersects(CollisionShape<?> other) {
        return switch (other) {
            case Sphere sphere -> this.intersectsSphere(sphere);
            case AABB aabb -> this.intersectsAABB(aabb);
            case OBB obb -> this.intersectsOBB(obb);
            default -> throw new IllegalStateException();
        };
    }

    public abstract boolean intersectsSphere(Sphere sphere);
    public abstract boolean intersectsAABB(AABB aabb);
    public abstract boolean intersectsOBB(OBB obb);

    public abstract Hit collideRay(Ray ray);
}
