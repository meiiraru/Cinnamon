package cinnamon.math.collision;

import org.joml.Vector3f;

public record Collision(Vector3f normal, float depth, CollisionShape shapeA, CollisionShape shapeB) {}
