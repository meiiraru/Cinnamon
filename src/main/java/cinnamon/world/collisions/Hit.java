package cinnamon.world.collisions;

public record Hit<T>(CollisionResult collision, T obj) {
    public T get() {
        return obj;
    }
}
