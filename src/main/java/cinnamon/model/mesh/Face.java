package cinnamon.model.mesh;

import java.util.List;

public class Face {

    private final List<Integer> vertices, uvs, normals, tangents;

    public Face(List<Integer> vertices, List<Integer> uvs, List<Integer> normals) {
        this(vertices, uvs, normals, List.of());
    }

    public Face(List<Integer> vertices, List<Integer> uvs, List<Integer> normals, List<Integer> tangents) {
        this.vertices = vertices;
        this.uvs = uvs;
        this.normals = normals;
        this.tangents = tangents;
    }

    public List<Integer> getVertices() {
        return vertices;
    }

    public List<Integer> getUVs() {
        return uvs;
    }

    public List<Integer> getNormals() {
        return normals;
    }

    public List<Integer> getTangents() {
        return tangents;
    }
}
