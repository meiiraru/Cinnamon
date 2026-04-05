package cinnamon.math.collision;

import cinnamon.math.Maths;
import org.joml.Math;
import org.joml.Vector3f;

public final class Resolution {

    public enum Mode {
        STICK, SLIDE, BOUNCE, FORCE, PUSH
    }

    // * sweep * //

    //completely stop at the intersection
    public static void stick(Hit hit, Vector3f velocity, Vector3f move) {
        //move up to the intersection
        float t = Maths.clamp(hit.tNear() - Maths.EPSILON, -Maths.EPSILON, 1f);
        move.mul(t);
        //completely kill all velocity
        velocity.set(0f);
    }

    //move up to the intersection and slide along the surface
    public static void slide(Hit hit, Vector3f velocity, Vector3f move) {
        Vector3f normal = hit.normal();

        //move up to the intersection
        float moveDot = move.dot(normal);
        if (moveDot < 0f) {
            float t = Maths.clamp(hit.tNear() - Maths.EPSILON, -Maths.EPSILON, 1f);
            float blocked = moveDot * (1f - t);
            move.sub(normal.x * blocked, normal.y * blocked, normal.z * blocked);
        }

        //remove the component of the velocity in the direction of the normal
        float velDot = velocity.dot(normal);
        if (velDot < 0f)
            velocity.sub(normal.x * velDot, normal.y * velDot, normal.z * velDot);
    }

    //reflect the remaining movement and velocity by the bounciness factor (0 = no bounce, 1 = perfect bounce)
    public static void bounce(Hit hit, Vector3f velocity, Vector3f move, float bounciness) {
        Vector3f normal = hit.normal();

        //reflect remaining movement
        float moveDot = move.dot(normal);
        if (moveDot < 0f) {
            float t = Maths.clamp(hit.tNear() - Maths.EPSILON, -Maths.EPSILON, 1f);
            float blocked = moveDot * (1f - t);
            float impulse = blocked * (1f + bounciness);
            move.sub(normal.x * impulse, normal.y * impulse, normal.z * impulse);
        }

        //reflect velocity using the normal
        float velDot = velocity.dot(normal);
        if (velDot < 0f) {
            float impulse = velDot * (1f + bounciness);
            velocity.sub(normal.x * impulse, normal.y * impulse, normal.z * impulse);
        }
    }

    //allow the intersection but apply a force to push the object away in the shortest direction out
    public static void force(Hit hit, Vector3f motion, Vector3f pushDelta, float pushFactor) {
        Vector3f normal = hit.normal();

        float moveDot = motion.dot(normal);
        if (moveDot < 0f) {
            float t = Maths.clamp(hit.tNear() - Maths.EPSILON, -Maths.EPSILON, 1f);
            float blocked = moveDot * (1f - t);
            float pushMag = -pushFactor * blocked;

            //subtract from velocity since normal points towards obstacle
            pushDelta.sub(normal.x * pushMag, normal.y * pushMag, normal.z * pushMag);
        }
    }

    //push the other object out of the way by the amount of movement blocked by the collision
    public static void push(Hit hit, Vector3f move, Vector3f pushMove) {
        Vector3f normal = hit.normal();

        float motionDot = move.dot(normal);
        if (motionDot < 0f) {
            float t = Maths.clamp(hit.tNear() - Maths.EPSILON, -Maths.EPSILON, 1f);
            float blocked = motionDot * (1f - t);
            //transfer the blocked motion into the other object
            pushMove.add(normal.x * blocked, normal.y * blocked, normal.z * blocked);
        }
    }


    // * overlap * //


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

        float lenSq = normal.lengthSquared();
        if (lenSq > Maths.KINDA_SMALL_NUMBER) {
            float invLen = 1f / Math.sqrt(lenSq);
            float pushMag = pushFactor * depth;

            //subtract from velocity since normal points towards obstacle
            velocity.sub(normal.x * invLen * pushMag, normal.y * invLen * pushMag, normal.z * invLen * pushMag);
        }
    }

    public static void push(Collision collision, Vector3f pushMove) {
        Vector3f normal = collision.normal();
        float depth = collision.depth();

        pushMove.add(normal.x * depth, normal.y * depth, normal.z * depth);
    }
}