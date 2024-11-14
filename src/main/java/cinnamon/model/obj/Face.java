package cinnamon.model.obj;

import java.util.List;

public class Face {

    private final List<Integer> vertices, uvs, normals;

    public Face(List<Integer> vertices, List<Integer> uvs, List<Integer> normals) {
        this.vertices = vertices;
        this.uvs = uvs;
        this.normals = normals;
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
}
