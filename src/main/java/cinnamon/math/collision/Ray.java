package cinnamon.math.collision;

import cinnamon.math.Maths;
import org.joml.Vector3f;

public class Ray {

    private final Vector3f origin = new Vector3f();
    private final Vector3f direction = new Vector3f(0, 0, 1);
    private float maxDistance = 1f;

    public Ray() {}

    public Ray(Vector3f origin, Vector3f direction) {
        this(origin, direction, 1f);
    }

    public Ray(Vector3f origin, Vector3f direction, float maxDistance) {
        this(origin.x, origin.y, origin.z, direction.x, direction.y, direction.z, maxDistance);
    }

    public Ray(float x, float y, float z, float dirX, float dirY, float dirZ) {
        this(x, y, z, dirX, dirY, dirZ, 1f);
    }

    public Ray(float x, float y, float z, float dirX, float dirY, float dirZ, float maxDistance) {
        this.setOrigin(x, y, z);
        this.setDirection(dirX, dirY, dirZ);
        this.setMaxDistance(maxDistance);
    }

    public Ray setOrigin(Vector3f origin) {
        return this.setOrigin(origin.x, origin.y, origin.z);
    }

    public Ray setOrigin(float x, float y, float z) {
        this.origin.set(x, y, z);
        return this;
    }

    public Ray setDirection(Vector3f direction) {
        return this.setDirection(direction.x, direction.y, direction.z);
    }

    public Ray setDirection(float x, float y, float z) {
        this.direction.set(x, y, z).normalize();
        return this;
    }

    public Ray setMaxDistance(float maxDistance) {
        this.maxDistance = Math.max(maxDistance, 0f);
        return this;
    }

    public Vector3f getOrigin() {
        return origin;
    }

    public Vector3f getDirection() {
        return direction;
    }

    public float getMaxDistance() {
        return maxDistance;
    }

    public static Hit collide(Ray ray, CollisionShape shape) {
        return ray.maxDistance >= Maths.SMALL_NUMBER ? shape.collideRay(ray) : null;
    }
}
