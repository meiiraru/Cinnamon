package cinnamon.world.collisions;

import org.joml.Vector3f;

public class CollisionResolver {

    public static void stick(CollisionResult collision, Vector3f motion, Vector3f move) {
        move.mul(collision.near());
        motion.set(0);
    }

    public static void slide(CollisionResult collision, Vector3f motion, Vector3f move) {
        float near = collision.near();
        Vector3f nor = collision.normal();

        if (Math.abs(nor.x) > 0) {
            move.x *= near;
            motion.x = 0;
        }
        if (Math.abs(nor.y) > 0) {
            move.y *= near;
            motion.y = 0;
        }
        if (Math.abs(nor.z) > 0) {
            move.z *= near;
            motion.z = 0;
        }
    }

    public static void bounce(CollisionResult collision, Vector3f motion, Vector3f move, Vector3f bounce) {
        float near = collision.near();
        Vector3f nor = collision.normal();

        if (Math.abs(nor.x) > 0) {
            move.x *= near;
            motion.x *= -bounce.x;
        }
        if (Math.abs(nor.y) > 0) {
            move.y *= near;
            motion.y *= -bounce.y;
        }
        if (Math.abs(nor.z) > 0) {
            move.z *= near;
            motion.z *= -bounce.z;
        }
    }

    public static void push(CollisionResult collision, Vector3f motion, Vector3f otherMotion) {
        float near = collision.near();
        Vector3f nor = collision.normal();

        if (Math.abs(nor.x) > 0) {
            otherMotion.x = motion.x - motion.x * near;
        }
        if (Math.abs(nor.y) > 0) {
            otherMotion.y = motion.y - motion.y * near;
        }
        if (Math.abs(nor.z) > 0) {
            otherMotion.z = motion.z - motion.z * near;
        }
    }

    public static void pushStick(CollisionResult collision, Vector3f motion, Vector3f otherMotion) {
        float near = collision.near();
        otherMotion.set(motion).sub(motion.x * near, motion.y * near, motion.z * near);
    }
}
