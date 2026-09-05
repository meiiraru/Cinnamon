package cinnamon.model;

import cinnamon.model.mesh.Face;
import cinnamon.model.mesh.Group;
import cinnamon.model.mesh.Mesh;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MeshHelper {

    public static void stripDuplicateVertices(Mesh mesh) {
        //store unique vertices
        Map<Vector3f, Integer> vertexMap  = new LinkedHashMap<>();
        Map<Vector2f, Integer> uvMap      = new LinkedHashMap<>();
        Map<Vector3f, Integer> normalMap  = new LinkedHashMap<>();
        Map<Vector3f, Integer> tangentMap = new LinkedHashMap<>();

        //original mesh data
        List<Vector3f> vertices = mesh.getVertices();
        List<Vector2f> uvs      = mesh.getUVs();
        List<Vector3f> normals  = mesh.getNormals();
        List<Vector3f> tangents = mesh.getTangents();

        //build set of unique vertices
        int i = 0;
        for (Vector3f vertex : vertices)
            if (!vertexMap.containsKey(vertex))
                vertexMap.put(vertex, i++);

        i = 0;
        for (Vector2f uv : uvs)
            if (!uvMap.containsKey(uv))
                uvMap.put(uv, i++);

        i = 0;
        for (Vector3f normal : normals)
            if (!normalMap.containsKey(normal))
                normalMap.put(normal, i++);

        i = 0;
        for (Vector3f tangent : tangents)
            if (!tangentMap.containsKey(tangent))
                tangentMap.put(tangent, i++);

        //update indexes on the faces
        for (Group group : mesh.getGroups()) {
            for (Face face : group.getFaces()) {
                //face indexes
                int[] faceVertices = face.getVertices();
                int[] faceUVs      = face.getUVs();
                int[] faceNormals  = face.getNormals();
                int[] faceTangents = face.getTangents();

                //remap the new indexes
                for (int j = 0; j < faceVertices.length; j++) {
                    Vector3f v = vertices.get(faceVertices[j]);
                    faceVertices[j] = vertexMap.get(v);
                }

                if (face.hasUVs()) {
                    for (int j = 0; j < faceUVs.length; j++) {
                        Vector2f uv = uvs.get(faceUVs[j]);
                        faceUVs[j] = uvMap.get(uv);
                    }
                }

                if (face.hasNormals()) {
                    for (int j = 0; j < faceNormals.length; j++) {
                        Vector3f n = normals.get(faceNormals[j]);
                        faceNormals[j] = normalMap.get(n);
                    }
                }

                if (face.hasTangents()) {
                    for (int j = 0; j < faceTangents.length; j++) {
                        Vector3f t = tangents.get(faceTangents[j]);
                        faceTangents[j] = tangentMap.get(t);
                    }
                }
            }
        }

        //update mesh data with the vertex set
        vertices.clear();
        vertices.addAll(vertexMap.keySet());
        uvs.clear();
        uvs.addAll(uvMap.keySet());
        normals.clear();
        normals.addAll(normalMap.keySet());
        tangents.clear();
        tangents.addAll(tangentMap.keySet());
    }

    public static void centerMesh(Mesh mesh) {
        Vector3f center = mesh.getBounds().getCenter();
        for (Vector3f vertex : mesh.getVertices())
            vertex.sub(center);
    }
}
