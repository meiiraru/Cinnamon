package mayo.model.obj;

public class Face {

    private final int[] vertices, uvs, normals;

    public Face(int[] vertices, int[] uvs, int[] normals) {
        this.vertices = vertices;
        this.uvs = uvs;
        this.normals = normals;
    }

    public int getVertexCount() {
        return vertices.length;
    }

    public int getLength() {
        return (vertices.length * 3) + (uvs.length * 2) + (normals.length * 3);
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
}
