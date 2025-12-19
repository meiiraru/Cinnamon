package cinnamon.world.terrain;

import cinnamon.model.Vertex;
import cinnamon.model.VertexHelper;
import cinnamon.registry.TerrainRegistry;
import cinnamon.render.Camera;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class PrimitiveTerrain extends Terrain {
    private final Vertex[][] vertices;

    public PrimitiveTerrain(Vertex[][] vertices) {
        this(vertices, true);
    }

    public PrimitiveTerrain(Vertex[][] vertices, boolean smooth) {
        super(null, TerrainRegistry.CUSTOM);
        this.vertices = smooth ? recalculateNormals(vertices) : vertices;
        this.preciseAABB.add(aabb);
        updateAABB();
    }

    @Override
    public void render(Camera camera, MatrixStack matrices, float delta) {
        VertexConsumer.WORLD_MAIN.consume(vertices);
    }

    @Override
    protected void updateAABB() {
        if (vertices == null || vertices.length == 0 || vertices[0].length == 0) {
            super.updateAABB();
            return;
        }

        float minX = Integer.MAX_VALUE; float maxX = Integer.MIN_VALUE;
        float minY = Integer.MAX_VALUE; float maxY = Integer.MIN_VALUE;
        float minZ = Integer.MAX_VALUE; float maxZ = Integer.MIN_VALUE;

        for (Vertex[] vertexArr : vertices) {
            for (Vertex vertex : vertexArr) {
                Vector3f pos = vertex.getPosition();
                if (pos.x < minX) minX = pos.x;
                if (pos.x > maxX) maxX = pos.x;
                if (pos.y < minY) minY = pos.y;
                if (pos.y > maxY) maxY = pos.y;
                if (pos.z < minZ) minZ = pos.z;
                if (pos.z > maxZ) maxZ = pos.z;
            }
        }

        this.aabb.set(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public Vertex[][] getVertices() {
        return vertices;
    }

    private static Vertex[][] recalculateNormals(Vertex[][] vertices) {
        Vertex[][] newVertices = new Vertex[vertices.length][];
        List<Vertex> vertexList = new ArrayList<>();

        //extract the vertices into a list
        for (int i = 0; i < vertices.length; i++) {
            Vertex[] face = vertices[i];
            newVertices[i] = new Vertex[face.length];

            for (int j = 0; j < face.length; j++) {
                Vertex vertex = face[j];
                Vertex newVertex = Vertex.of(vertex.getPosition()).color(vertex.getColor()).normal(vertex.getNormal());
                newVertices[i][j] = newVertex;
            }

            //triangulate because the normal calculation is based on triangles
            vertexList.addAll(VertexHelper.triangulate(new ArrayList<>(List.of(newVertices[i]))));
        }

        //smooth normals
        VertexHelper.calculateFlatNormals(vertexList);
        VertexHelper.smoothNormals(vertexList, 45f);

        //new vertices will update with the same references as the vertexList, so just return it
        return newVertices;
    }
}
