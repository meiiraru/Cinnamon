package mayo.model.obj;

import mayo.render.shader.Attributes;

import java.util.List;

public class Face {

    private final List<Integer> vertices, uvs, normals;

    public Face(List<Integer> vertices, List<Integer> uvs, List<Integer> normals) {
        this.vertices = vertices;
        this.uvs = uvs;
        this.normals = normals;
    }

    public int getVertexCount() {
        return vertices.size();
    }

    public int getLength() {
        return (vertices.size() * 3) + (uvs.size() * 2) + (normals.size() * 3);
    }

    public int getAttributesFlag() {
        //always present
        int flags = Attributes.POS;

        if (!uvs.isEmpty())
            flags |= Attributes.UV;
        if (!normals.isEmpty())
            flags |= Attributes.NORMAL;

        return flags;
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
