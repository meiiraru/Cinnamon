package cinnamon.parsers;

import cinnamon.model.Vertex;
import cinnamon.model.material.Material;
import cinnamon.model.obj.Face;
import cinnamon.model.obj.Group;
import cinnamon.model.obj.Mesh;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class VerticesToMesh {

    public static Mesh fromVertices(Vertex[][] vertices) {
        return fromVertices(vertices, null);
    }

    public static Mesh fromVertices(Vertex[][] vertices, Material material) {
        return fromVertices(vertices, material, true, true, "default");
    }

    public static Mesh fromVertices(Vertex[][] vertices, Material material, boolean includeUVs, boolean includeNormals, String name) {
        Mesh mesh = new Mesh();
        Group group = new Group(name);
        mesh.getGroups().add(group);

        if (material != null) {
            mesh.getMaterials().put(material.getName(), material);
            group.setMaterial(material);
        }

        List<Vector3f> positions = mesh.getVertices();
        List<Vector2f> uvs = mesh.getUVs();
        List<Vector3f> normals = mesh.getNormals();

        //faces
        for (Vertex[] f : vertices) {
            List<Integer> posIndices = new ArrayList<>();
            List<Integer> uvIndices = new ArrayList<>();
            List<Integer> normIndices = new ArrayList<>();

            //vertex
            for (Vertex vertex : f) {
                positions.add(vertex.getPosition());
                posIndices.add(positions.size() - 1);

                if (includeUVs) {
                    uvs.add(vertex.getUV());
                    uvIndices.add(uvs.size() - 1);
                }

                if (includeNormals) {
                    normals.add(vertex.getNormal());
                    normIndices.add(normals.size() - 1);
                }
            }

            Face face = new Face(posIndices, uvIndices, normIndices);
            group.getFaces().add(face);
        }

        //return the mesh
        return mesh;
    }
}
