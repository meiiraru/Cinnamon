package cinnamon.math.collision;

import org.joml.Vector3f;

public final class Collision {

    private final Vector3f normal;
    private final float depth;
    private Collider<?> shapeA, shapeB;

    public Collision(Vector3f normal, float depth, Collider<?> shapeA, Collider<?> shapeB) {
        this.normal = normal;
        this.depth = depth;
        this.shapeA = shapeA;
        this.shapeB = shapeB;
    }

    public Collision invert() {
        this.normal.negate();
        Collider<?> temp = this.shapeA;
        this.shapeA = this.shapeB;
        this.shapeB = temp;
        return this;
    }

    public Vector3f normal() {
        return normal;
    }

    public float depth() {
        return depth;
    }

    public Collider<?> shapeA() {
        return shapeA;
    }

    public Collider<?> shapeB() {
        return shapeB;
    }
}
