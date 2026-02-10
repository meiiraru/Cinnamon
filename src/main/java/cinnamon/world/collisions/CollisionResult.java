package cinnamon.world.collisions;

import org.joml.Vector3f;

public record CollisionResult(float near, Vector3f normal, Vector3f pos) {}
