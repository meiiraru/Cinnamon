package mayo.model;

public class Face {

    private final int[] vertices, uvs, normals;

    public Face(int[] vertices, int[] uvs, int[] normals) {
        this.vertices = vertices;
        this.uvs = uvs;
        this.normals = normals;
    }
}
