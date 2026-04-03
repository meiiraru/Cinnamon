package cinnamon.math.collision;

import org.joml.Vector3f;

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
public record Hit(Vector3f position, Vector3f normal, float tNear, float tFar, Ray ray, Collider<?> collider) implements Comparable<Hit> {
    @Override
    public int compareTo(Hit o) {
        return Float.compare(this.tNear, o.tNear);
    }
}
