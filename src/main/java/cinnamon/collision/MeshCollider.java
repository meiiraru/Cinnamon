package cinnamon.collision;

import org.joml.Vector3f;

public class MeshCollider extends Collider {

    private final Vector3f[] vertices;

    public MeshCollider(Vector3f... vertices) {
        this.vertices = vertices;
    }

    @Override
    protected Vector3f findFurthestPoint(float dirX, float dirY, float dirZ) {
        Vector3f maxPoint = null;
        float maxDistance = Float.NEGATIVE_INFINITY;

        for (Vector3f vertex : vertices) {
            float distance = vertex.dot(dirX, dirY, dirZ);
            if (distance > maxDistance) {
                maxDistance = distance;
                maxPoint = new Vector3f(vertex);
            }
        }

        return maxPoint;
    }

    public Vector3f[] getVertices() {
        return vertices;
    }
}
