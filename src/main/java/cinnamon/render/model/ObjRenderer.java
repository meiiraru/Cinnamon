package cinnamon.render.model;

import cinnamon.model.Vertex;
import cinnamon.model.VertexHelper;
import cinnamon.model.obj.Face;
import cinnamon.model.obj.Group;
import cinnamon.model.obj.Mesh;
import cinnamon.utils.AABB;
import cinnamon.utils.Pair;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ObjRenderer extends ModelRenderer {

    private final Mesh mesh;

    public ObjRenderer(ObjRenderer other) {
        super(other.meshes);
        this.aabb.set(other.aabb);
        this.mesh = other.mesh;
    }

    public ObjRenderer(Mesh mesh) {
        super(new HashMap<>(mesh.getGroups().size(), 1f));
        this.mesh = mesh;
        bakeModel();
    }

    protected void bakeModel() {
        Vector3f bbMin = new Vector3f(Integer.MAX_VALUE);
        Vector3f bbMax = new Vector3f(Integer.MIN_VALUE);

        //grab mesh data
        List<Vector3f> vertices = mesh.getVertices();
        List<Vector2f> uvs = mesh.getUVs();
        List<Vector3f> normals = mesh.getNormals();

        //iterate groups
        for (Group group : mesh.getGroups()) {
            //vertex list and capacity
            List<Vertex> sortedVertices = new ArrayList<>();

            //group min and max
            Vector3f groupMin = new Vector3f(Integer.MAX_VALUE);
            Vector3f groupMax = new Vector3f(Integer.MIN_VALUE);

            //iterate faces
            for (Face face : group.getFaces()) {
                //indexes
                List<Integer> v = face.getVertices();
                List<Integer> vt = face.getUVs();
                List<Integer> vn = face.getNormals();

                //vertex list
                List<Vertex> data = new ArrayList<>();

                for (int i = 0; i < v.size(); i++) {
                    //parse indexes to their actual values
                    Vector3f a = vertices.get(v.get(i));
                    Vector2f b = !vt.isEmpty() ? uvs.get(vt.get(i)) : Vertex.DEFAULT_UV;
                    Vector3f c = !vn.isEmpty() ? normals.get(vn.get(i)) : Vertex.DEFAULT_NORMAL;

                    //calculate min and max
                    bbMin.min(a);
                    bbMax.max(a);
                    groupMin.min(a);
                    groupMax.max(a);

                    //add to vertex list
                    data.add(Vertex.of(a).uv(b).normal(c));
                }

                //triangulate the faces using ear clipping
                List<Vertex> sorted = VertexHelper.triangulate(data);

                //add data to the vertex list
                sortedVertices.addAll(sorted);
            }

            //skip empty groups
            if (sortedVertices.isEmpty())
                continue;

            //default angle threshold for smoothing
            float angleThreshold = 45f;

            //generate normals when missing
            if (normals.isEmpty()) {
                VertexHelper.calculateFlatNormals(sortedVertices);
                VertexHelper.smoothNormals(sortedVertices, angleThreshold);
            }

            //calculate tangents
            if (!uvs.isEmpty())
                VertexHelper.calculateTangents(sortedVertices, angleThreshold);

            //strip the unique indices from the vertex list
            Pair<int[], List<Vertex>> indices = VertexHelper.stripIndices(sortedVertices);

            //create a new group with the OpenGL attributes
            MeshData groupData = generateMesh(new AABB(groupMin, groupMax), indices.second(), indices.first(), group.getMaterial());

            String groupName = group.getName();
            String newName = groupName;
            for (int i = 1; this.meshes.containsKey(newName); i++)
                newName = groupName + "_" + i;

            this.meshes.put(newName, groupData);
        }

        this.aabb.set(bbMin, bbMax);
    }

    public Mesh getMesh() {
        return mesh;
    }
}
