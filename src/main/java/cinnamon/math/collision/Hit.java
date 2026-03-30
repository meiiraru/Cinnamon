package cinnamon.math.collision;

import cinnamon.math.Maths;
import org.joml.Vector3f;

public record Hit(Vector3f position, Vector3f normal, float tNear, float tFar, Ray ray, CollisionShape shape) implements Comparable<Hit> {
    public float nearScalar() {
        return tNear / ray.getMaxDistance();
    }

    public float safeNearScalar() {
        return Math.max(0f, tNear - Maths.EPSILON) / ray.getMaxDistance();
    }

    public float farScalar() {
        return tFar / ray.getMaxDistance();
    }

    @Override
    public int compareTo(Hit o) {
        return Float.compare(this.tNear, o.tNear);
    }
}
