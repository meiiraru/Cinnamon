package cinnamon.model.mesh;

public class Face {

    private final int[] vertices;
    private final int[] uvs;
    private final int[] normals;
    private final int[] tangents;

    public Face(int[] vertices, int[] uvs, int[] normals) {
        this(vertices, uvs, normals, null);
    }

    public Face(int[] vertices, int[] uvs, int[] normals, int[] tangents) {
        this.vertices = vertices;
        this.uvs = uvs;
        this.normals = normals;
        this.tangents = tangents;
    }

    public int[] getVertices() {
        return vertices;
    }

    public int[] getUVs() {
        return uvs;
    }

    public int[] getNormals() {
        return normals;
    }

    public int[] getTangents() {
        return tangents;
    }

    public boolean hasUVs() {
        return uvs != null && uvs.length > 0;
    }

    public boolean hasNormals() {
        return normals != null && normals.length > 0;
    }

    public boolean hasTangents() {
        return tangents != null && tangents.length > 0;
    }
}
