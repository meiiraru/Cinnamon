package cinnamon.math.collision;

import org.joml.Vector3f;

public abstract class CollisionShape<T extends CollisionShape<T>> {

    public abstract T clone();

    public abstract Vector3f getCenter();

    public T setCenter(Vector3f center) {
        return this.setCenter(center.x, center.y, center.z);
    }
    public abstract T setCenter(float x, float y, float z);

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

    public Vector3f closestPoint(Vector3f point, Vector3f out) {
        return this.closestPoint(point.x, point.y, point.z, out);
    }
    public abstract Vector3f closestPoint(float x, float y, float z, Vector3f out);

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

    public abstract void project(Vector3f axis, float[] minMax);

    public Collision collide(CollisionShape<?> other) {
        return switch (other) {
            case Sphere sphere -> this.collideSphere(sphere);
            case AABB aabb -> this.collideAABB(aabb);
            case OBB obb -> this.collideOBB(obb);
            default -> throw new IllegalStateException();
        };
    }

    public abstract Collision collideSphere(Sphere sphere);
    public abstract Collision collideAABB(AABB aabb);
    public abstract Collision collideOBB(OBB obb);
}