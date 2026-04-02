package cinnamon.math.collision;

import cinnamon.math.Maths;
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


    // * resolutions * //


    public enum Mode {
        STICK, SLIDE, BOUNCE, FORCE, PUSH
    }

    public static void stick(Collision collision, Vector3f velocity, Vector3f move) {
        Vector3f normal = collision.normal();
        float depth = collision.depth();

        //push out of the intersection
        move.sub(normal.x * depth, normal.y * depth, normal.z * depth);
        //completely kill all velocity
        velocity.set(0f);
    }

    public static void slide(Collision collision, Vector3f velocity, Vector3f move) {
        Vector3f normal = collision.normal();
        float depth = collision.depth();

        move.sub(normal.x * depth, normal.y * depth, normal.z * depth);

        //remove the component of the velocity in the direction of the normal
        float dot = velocity.dot(normal);
        if (dot > 0f)
            velocity.sub(normal.x * dot, normal.y * dot, normal.z * dot);
    }

    public static void bounce(Collision collision, Vector3f velocity, Vector3f move, float bounciness) {
        Vector3f normal = collision.normal();
        float depth = collision.depth();

        move.add(-normal.x * depth, -normal.y * depth, -normal.z * depth);

        //reflect velocity using the normal
        float dot = velocity.dot(normal);
        if (dot > 0f) {
            float impulse = dot * (1f + bounciness);
            velocity.sub(normal.x * impulse, normal.y * impulse, normal.z * impulse);
        }
    }

    public static void force(Collision collision, Vector3f velocity, float pushFactor) {
        Vector3f normal = collision.normal();
        float depth = collision.depth();

        float lenSq = normal.x * normal.x + normal.z * normal.z;
        if (lenSq > Maths.KINDA_SMALL_NUMBER) {
            float invLen = 1f / (float) Math.sqrt(lenSq);
            float pushMag = pushFactor * depth;

            //subtract from velocity since normal points towards obstacle
            velocity.sub(normal.x * invLen * pushMag, 0f, normal.z * invLen * pushMag);
        }
    }

    public static void push(Collision collision, Vector3f pushMove) {
        Vector3f normal = collision.normal();
        float depth = collision.depth();

        pushMove.add(normal.x * depth, normal.y * depth, normal.z * depth);
    }
}
