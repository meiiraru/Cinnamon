package cinnamon.parsers;

import cinnamon.model.Vertex;
import cinnamon.model.material.Material;
import cinnamon.model.mesh.Face;
import cinnamon.model.mesh.Group;
import cinnamon.model.mesh.Mesh;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.List;

public class VerticesToMesh {

    public static Mesh fromVertices(Vertex[][] vertices) {
        return fromVertices(vertices, null);
    }

    public static Mesh fromVertices(Vertex[][] vertices, Material material) {
        return fromVertices(vertices, material, true, true, false, "default");
    }

    public static Mesh fromVertices(Vertex[][] vertices, Material material, boolean includeUVs, boolean includeNormals, boolean includeTangents, String name) {
        Mesh mesh = new Mesh();
        Group group = new Group(name);
        mesh.getGroups().add(group);

        if (material != null) {
            mesh.getMaterials().put(material.getName(), material);
            group.setMaterial(material);
        }

        if (vertices.length > 0 && vertices[0].length > 0) {
            mesh.getBounds().set(vertices[0][0].getPos());
            group.getBounds().set(vertices[0][0].getPos());
        }

        List<Vector3f> positions = mesh.getVertices();
        List<Vector2f> uvs = mesh.getUVs();
        List<Vector3f> normals = mesh.getNormals();
        List<Vector3f> tangents = mesh.getTangents();

        //faces
        for (Vertex[] f : vertices) {
            int[] posIndices  = new int[f.length];
            int[] uvIndices   = includeUVs ? new int[f.length] : null;
            int[] normIndices = includeNormals ? new int[f.length] : null;
            int[] tanIndices  = includeTangents ? new int[f.length] : null;

            //vertex
            for (int i = 0; i < f.length; i++) {
                Vertex vertex = f[i];

                positions.add(vertex.getPos());
                posIndices[i] = positions.size() - 1;

                if (includeUVs) {
                    uvs.add(vertex.getUV());
                    uvIndices[i] = uvs.size() - 1;
                }

                if (includeNormals) {
                    normals.add(vertex.getNormal());
                    normIndices[i] = normals.size() - 1;
                }

                if (includeTangents) {
                    tangents.add(vertex.getTangent());
                    tanIndices[i] = tangents.size() - 1;
                }

                mesh.getBounds().include(vertex.getPos());
                group.getBounds().include(vertex.getPos());
            }

            Face face = new Face(posIndices, uvIndices, normIndices, tanIndices);
            group.getFaces().add(face);
        }

        //return the mesh
        return mesh;
    }
}
