package cinnamon.math.collision;

import cinnamon.math.Maths;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class CollisionSolver {

    public static final int
            DEFAULT_SWEEP_ITERATIONS   = 5,
            DEFAULT_OVERLAP_ITERATIONS = 3;
    public static final ResolutionParams
            DEFAULT_PARAMS = new ResolutionParams(1f, 1f, 1f);


    // * overlap * //


    public static void resolveOverlaps(Collider<?> target, List<Collider<?>> obstacles, Vector3f velocity, Resolution.Mode mode, ResolutionParams params, int maxIterations) {
        //check for a valid resolution mode
        if (mode == null)
            return;

        ResolutionParams p = params != null ? params : DEFAULT_PARAMS;

        //we store the correction to apply to the target after checking against all obstacles
        Vector3f correction = new Vector3f();

        //try to resolve overlaps sequentially
        for (int i = 0; i < maxIterations; i++) {
            boolean overlapping = false;

            //check for overlaps with all obstacles and accumulate the correction vector
            for (Collider<?> obstacle : obstacles) {
                //do the collision check
                Collision collision = target.collide(obstacle);
                if (collision == null)
                    continue;

                //check for the contact resolution
                overlapping = true;
                applyOverlapContact(collision, mode, p, velocity, correction);

                //if the correction is meaningless, skip
                if (correction.lengthSquared() < Maths.KINDA_SMALL_NUMBER)
                    continue;

                //flag that we overlapped and immediately apply the correction
                target.translate(correction);
                correction.set(0f);
            }

            //if there were no overlaps, we can stop the resolution process
            if (!overlapping)
                return;
        }
    }

    private static void applyOverlapContact(Collision collision, Resolution.Mode mode, ResolutionParams params, Vector3f velocity, Vector3f correction) {
        Vector3f normal = collision.normal();
        float depth = collision.depth();

        switch (mode) {
            //move the target out of the obstacle, but void all velocity
            case STICK -> {
                correction.sub(normal.x * depth, normal.y * depth, normal.z * depth);
                velocity.set(0f);
            }
            //move the target out of the obstacle
            case SLIDE -> {
                correction.sub(normal.x * depth, normal.y * depth, normal.z * depth);
                clipOverlapVelocity(velocity, normal);
            }
            //move the target out of the obstacle and reflect the velocity
            case BOUNCE -> {
                correction.sub(normal.x * depth, normal.y * depth, normal.z * depth);
                reflectOverlapVelocity(velocity, normal, params.bounciness());
            }
            //apply overlap force away from the obstacle normal
            case FORCE -> {
                float pushMag = depth * params.forceFactor();
                velocity.sub(normal.x * pushMag, normal.y * pushMag, normal.z * pushMag);
            }
            //pushes the obstacle out of the target when possible
            case PUSH -> {
                float pushMag = depth * params.pushFactor();
                collision.shapeB().translate(normal.x * pushMag, normal.y * pushMag, normal.z * pushMag);
            }
        }
    }


    // * sweep * //


    public static SweepResult resolveSweep(Collider<?> target, List<Collider<?>> obstacles, Vector3f velocity, Resolution.Mode mode, ResolutionParams params, int maxIterations) {
        //prepare the result object and check for a resolution mode
        SweepResult result = new SweepResult();
        if (mode == null)
            return result;

        ResolutionParams p = params != null ? params : DEFAULT_PARAMS;

        //we need to keep track of the movement still left in this frame
        Vector3f remainingVel = new Vector3f(velocity);

        //resolve sequential contacts using only the movement still left in this frame
        for (int i = 0; i < maxIterations; i++) {
            //no substantial movement left, so we can stop the resolution process
            if (remainingVel.lengthSquared() < Maths.KINDA_SMALL_NUMBER)
                break;

            //collect the collisions for the movement still in this frame
            List<Hit> contacts = collectEarliestHits(target, obstacles, remainingVel);
            if (contacts.isEmpty()) {
                //if it is empty, we can just move the target by the remaining movement and stop the resolution process
                target.translate(remainingVel);
                break;
            }

            //move the target up to the first contact
            float tNear = Math.max(contacts.getFirst().tNear() - Maths.EPSILON, 0f);
            if (tNear > 0f)
                target.translate(remainingVel.x * tNear, remainingVel.y * tNear, remainingVel.z * tNear);

            //update the movement still left in this frame by removing the portion that we just moved
            remainingVel.mul(Math.max(1f - tNear, 0f));

            //apply the contact resolution for each contact and add it to the result object
            for (Hit contact : contacts) {
                result.hits.add(contact);
                applySweepContact(target, contact, mode, p, velocity, remainingVel, result);
            }

            result.collisionPasses++;
        }

        return result;
    }

    public static List<Hit> collectEarliestHits(Collider<?> target, List<Collider<?>> obstacles, Vector3f velocity) {
        //store in a list the hits with the earliest time of impact
        List<Hit> nearest = new ArrayList<>();
        float nearestT = Float.POSITIVE_INFINITY;

        //check for collisions with all obstacles
        for (Collider<?> obstacle : obstacles) {
            //check for a collision
            Hit hit = target.sweep(obstacle, velocity);
            if (hit == null)
                continue;

            //check for its time of impact
            float t = hit.tNear();
            if (t < -Maths.EPSILON || t > 1f + Maths.EPSILON)
                continue;

            if (t < nearestT - Maths.EPSILON) {
                //if it is the earliest one so far, clear the list and add this one
                nearestT = t;
                nearest.clear();
                nearest.add(hit);
            } else if (Math.abs(t - nearestT) <= Maths.EPSILON) {
                //otherwise stack it with the other hits that have the same time of impact
                nearest.add(hit);
            }
        }

        return nearest;
    }

    private static void applySweepContact(Collider<?> target, Hit contact, Resolution.Mode mode, ResolutionParams params, Vector3f velocity, Vector3f remainingVel, SweepResult result) {
        Vector3f normal = contact.normal();
        switch (mode) {
            //void all movement and velocity along the normal
            case STICK -> {
                remainingVel.set(0f);
                velocity.set(0f);
            }
            //slide along the surface
            case SLIDE -> {
                clipAgainstNormal(remainingVel, normal);
                clipAgainstNormal(velocity, normal);
            }
            //bounce off the surface
            case BOUNCE -> {
                reflectAgainstNormal(remainingVel, normal, params.bounciness());
                reflectAgainstNormal(velocity, normal, params.bounciness());
            }
            //allow intersection regardless of the collision, the overlap will handle the correction
            case FORCE -> {
                target.translate(remainingVel);
                remainingVel.set(0f);
            }
            //transfer blocked motion into the hit obstacle when possible
            case PUSH -> {
                float moveDot = remainingVel.dot(normal);
                if (moveDot >= 0f)
                    return;

                float pushMag = moveDot * params.pushFactor();
                Vector3f pushDelta = new Vector3f(normal).mul(pushMag);
                Collider<?> hitCollider = contact.collider();

                hitCollider.translate(pushDelta);
                result.pushes.add(new Push(hitCollider, pushDelta));
            }
        }
    }


    // * helpers * //


    private static void clipAgainstNormal(Vector3f vec, Vector3f normal) {
        float dot = vec.dot(normal);
        if (dot < 0f)
            vec.sub(normal.x * dot, normal.y * dot, normal.z * dot);
    }

    private static void clipOverlapVelocity(Vector3f vec, Vector3f normal) {
        float dot = vec.dot(normal);
        if (dot > 0f)
            vec.sub(normal.x * dot, normal.y * dot, normal.z * dot);
    }

    private static void reflectAgainstNormal(Vector3f vec, Vector3f normal, float bounciness) {
        float dot = vec.dot(normal);
        if (dot < 0f) {
            float impulse = dot * (1f + bounciness);
            vec.sub(normal.x * impulse, normal.y * impulse, normal.z * impulse);
        }
    }

    private static void reflectOverlapVelocity(Vector3f vec, Vector3f normal, float bounciness) {
        float dot = vec.dot(normal);
        if (dot > 0f) {
            float impulse = dot * (1f + bounciness);
            vec.sub(normal.x * impulse, normal.y * impulse, normal.z * impulse);
        }
    }


    // * data classes * //


    public record ResolutionParams(float bounciness, float forceFactor, float pushFactor) {
        public ResolutionParams {
            bounciness = Math.max(bounciness, 0f);
            forceFactor = Math.max(forceFactor, 0f);
            pushFactor = Math.max(pushFactor, 0f);
        }
    }

    public record Push(Collider<?> collider, Vector3f delta) {}

    public static final class SweepResult {
        private final List<Hit> hits = new ArrayList<>();
        private final List<Push> pushes = new ArrayList<>();
        private int collisionPasses;

        public List<Hit> hits() {
            return hits;
        }

        public int collisionPasses() {
            return collisionPasses;
        }

        public List<Push> pushes() {
            return pushes;
        }
    }
}

