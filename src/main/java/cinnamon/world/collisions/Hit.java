package cinnamon.world.collisions;

import org.joml.Vector3f;

public record Hit<T>(CollisionResult collision, T obj, Vector3f pos) {
    public T get() {
        return obj;
    }
}
