package cinnamon.math.collision;

import org.joml.Vector3f;

public final class Hit implements Comparable<Hit> {

    private final Vector3f position;
    private final Vector3f normal;
    private final float tNear;
    private final float tFar;
    private final Ray ray;
    private Collider<?> collider;

    /**
     * Represents a hit result from a raycast or collision detection
     *
     * @param position The point of intersection in world space
     * @param normal The surface normal at the point of intersection
     * @param tNear The normalized distance from the ray origin to the near intersection point
     * @param tFar The normalized distance from the ray origin to the far intersection point
     * @param ray The ray that caused the hit
     * @param collider The collider that was hit
     */
    public Hit(Vector3f position, Vector3f normal, float tNear, float tFar, Ray ray, Collider<?> collider) {
        this.position = position;
        this.normal = normal;
        this.tNear = tNear;
        this.tFar = tFar;
        this.ray = ray;
        this.collider = collider;
    }

    public Vector3f position() {
        return position;
    }

    public Vector3f normal() {
        return normal;
    }

    public float tNear() {
        return tNear;
    }

    public float tFar() {
        return tFar;
    }

    public Ray ray() {
        return ray;
    }

    public Collider<?> collider() {
        return collider;
    }

    public Hit setCollider(Collider<?> collider) {
        this.collider = collider;
        return this;
    }

    @Override
    public int compareTo(Hit o) {
        return Float.compare(this.tNear, o.tNear);
    }
}
