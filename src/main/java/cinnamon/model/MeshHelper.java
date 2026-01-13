package cinnamon.model;

import cinnamon.model.obj.Face;
import cinnamon.model.obj.Group;
import cinnamon.model.obj.Mesh;
import cinnamon.utils.AABB;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MeshHelper {

    public static void stripDuplicateVertices(Mesh mesh) {
        //store unique vertices
        Map<Vector3f, Integer> vertexMap = new LinkedHashMap<>();
        Map<Vector2f, Integer> uvMap     = new LinkedHashMap<>();
        Map<Vector3f, Integer> normalMap = new LinkedHashMap<>();

        //original mesh data
        List<Vector3f> vertices = mesh.getVertices();
        List<Vector2f> uvs      = mesh.getUVs();
        List<Vector3f> normals  = mesh.getNormals();

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

        //update indexes on the faces
        for (Group group : mesh.getGroups()) {
            for (Face face : group.getFaces()) {
                //new face indexes
                List<Integer> newFaceVertices = new ArrayList<>();
                List<Integer> newFaceUVs      = new ArrayList<>();
                List<Integer> newFaceNormals  = new ArrayList<>();

                //new face indexes
                List<Integer> faceVertices = face.getVertices();
                List<Integer> faceUVs      = face.getUVs();
                List<Integer> faceNormals  = face.getNormals();

                //find the new indexes
                for (Integer vertex : faceVertices) {
                    Vector3f v = vertices.get(vertex);
                    newFaceVertices.add(vertexMap.get(v));
                }

                for (Integer uv : faceUVs) {
                    Vector2f u = uvs.get(uv);
                    newFaceUVs.add(uvMap.get(u));
                }

                for (Integer normal : faceNormals) {
                    Vector3f n = normals.get(normal);
                    newFaceNormals.add(normalMap.get(n));
                }

                //remap indexes
                faceVertices.clear();
                faceVertices.addAll(newFaceVertices);
                faceUVs.clear();
                faceUVs.addAll(newFaceUVs);
                faceNormals.clear();
                faceNormals.addAll(newFaceNormals);
            }
        }

        //update mesh data with the vertex set
        vertices.clear();
        vertices.addAll(vertexMap.keySet());
        uvs.clear();
        uvs.addAll(uvMap.keySet());
        normals.clear();
        normals.addAll(normalMap.keySet());
    }

    public static void centerMesh(Mesh mesh) {
        AABB aabb = calculateAABB(mesh);
        Vector3f center = aabb.getCenter();

        for (Vector3f vertex : mesh.getVertices())
            vertex.sub(center);
    }

    public static AABB calculateAABB(Mesh mesh) {
        Vector3f bbMin = new Vector3f(Integer.MAX_VALUE);
        Vector3f bbMax = new Vector3f(Integer.MIN_VALUE);

        for (Vector3f vertex : mesh.getVertices()) {
            bbMin.min(vertex);
            bbMax.max(vertex);
        }

        return new AABB(bbMin, bbMax);
    }
}
