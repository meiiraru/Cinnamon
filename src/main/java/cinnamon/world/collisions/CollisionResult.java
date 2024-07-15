package cinnamon.world.collisions;

import org.joml.Vector3f;

public record CollisionResult(float near, float far, Vector3f normal) {}
