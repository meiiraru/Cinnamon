package cinnamon.math.collision;

import cinnamon.math.collision.shape.*;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public abstract class Collider<T extends Collider<T>> {

    public abstract T clone();
    public abstract AABB toAABB();

    public abstract Vector3f getCenter();

    public T setCenter(Vector3f center) {
        return this.setCenter(center.x, center.y, center.z);
    }
    public abstract T setCenter(float x, float y, float z);

    public abstract float getVolume();

    public abstract Vector3f getRandomPoint(Vector3f out);

    public T translate(Vector3f translation) {
        return this.translate(translation.x, translation.y, translation.z);
    }
    public abstract T translate(float x, float y, float z);

    public abstract T applyMatrix(Matrix4f matrix);

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

    public abstract void project(Vector3f axis, float[] minMax);


    // -- tests -- //


    public abstract Hit rayCast(Ray ray);

    public boolean intersects(Collider<?> other) {
        return switch (other) {
            case Sphere sphere     -> this.intersects(sphere);
            case AABB aabb         -> this.intersects(aabb);
            case OBB obb           -> this.intersects(obb);
            case Plane plane       -> this.intersects(plane);
            case Triangle triangle -> this.intersects(triangle);
            case MeshCollider mesh -> this.intersects(mesh);
            default -> throw new UnsupportedOperationException("Collider type not supported: " + other.getClass().getSimpleName());
        };
    }

    public abstract boolean intersects(Sphere sphere);
    public abstract boolean intersects(AABB aabb);
    public abstract boolean intersects(OBB obb);
    public abstract boolean intersects(Plane plane);
    public abstract boolean intersects(Triangle triangle);
    public abstract boolean intersects(MeshCollider mesh);

    public Collision collide(Collider<?> other) {
        return switch (other) {
            case Sphere sphere     -> this.collide(sphere);
            case AABB aabb         -> this.collide(aabb);
            case OBB obb           -> this.collide(obb);
            case Plane plane       -> this.collide(plane);
            case Triangle triangle -> this.collide(triangle);
            case MeshCollider mesh -> this.collide(mesh);
            default -> throw new UnsupportedOperationException("Collider type not supported: " + other.getClass().getSimpleName());
        };
    }

    public abstract Collision collide(Sphere sphere);
    public abstract Collision collide(AABB aabb);
    public abstract Collision collide(OBB obb);
    public abstract Collision collide(Plane plane);
    public abstract Collision collide(Triangle triangle);
    public abstract Collision collide(MeshCollider mesh);

    protected Collision invertCollide(Collision result) {
        return result != null ? result.invert() : null;
    }

    public Hit sweep(Collider<?> other, Vector3f velocity) {
        return switch (other) {
            case Sphere sphere     -> this.sweep(sphere, velocity);
            case AABB aabb         -> this.sweep(aabb, velocity);
            case OBB obb           -> this.sweep(obb, velocity);
            case Plane plane       -> this.sweep(plane, velocity);
            case Triangle triangle -> this.sweep(triangle, velocity);
            case MeshCollider mesh -> this.sweep(mesh, velocity);
            default -> throw new UnsupportedOperationException("Collider type not supported: " + other.getClass().getSimpleName());
        };
    }

    public abstract Hit sweep(Sphere sphere, Vector3f velocity);
    public abstract Hit sweep(AABB aabb, Vector3f velocity);
    public abstract Hit sweep(OBB obb, Vector3f velocity);
    public abstract Hit sweep(Plane plane, Vector3f velocity);
    public abstract Hit sweep(Triangle triangle, Vector3f velocity);
    public abstract Hit sweep(MeshCollider mesh, Vector3f velocity);

    protected Hit invertSweep(Hit result, Collider<?> collider, Vector3f velocity) {
        if (result == null)
            return null;

        result.normal().negate();
        result.ray().invert();
        result.position().add(velocity.x * result.tNear(), velocity.y * result.tNear(), velocity.z * result.tNear());
        return result.setCollider(collider);
    }
}