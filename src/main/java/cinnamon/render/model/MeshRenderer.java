package cinnamon.render.model;

import cinnamon.model.Vertex;
import cinnamon.model.VertexHelper;
import cinnamon.model.material.Material;
import cinnamon.model.mesh.Face;
import cinnamon.model.mesh.Group;
import cinnamon.model.mesh.Mesh;
import cinnamon.utils.Pair;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cinnamon.events.Events.LOGGER;

public class MeshRenderer extends ModelRenderer {

    private final Mesh mesh;

    public MeshRenderer(MeshRenderer other) {
        super(other.meshes);
        this.aabb.set(other.aabb);
        this.mesh = other.mesh;
    }

    public MeshRenderer(Mesh mesh) {
        super(new HashMap<>(mesh.getGroups().size(), 1f));
        this.mesh = mesh;
        bakeModel();
    }

    protected void bakeModel() {
        //grab mesh data
        List<Vector3f> vertices = mesh.getVertices();
        List<Vector2f> uvs = mesh.getUVs();
        List<Vector3f> normals = mesh.getNormals();
        List<Vector3f> tangents = mesh.getTangents();

        //iterate groups
        for (Group group : mesh.getGroups()) {
            LOGGER.debug("Baking group \"%s\"", group.getName());

            //vertex list and capacity
            List<Vertex> sortedVertices = new ArrayList<>();

            //iterate faces
            for (Face face : group.getFaces()) {
                //indexes
                int[] v = face.getVertices();
                int[] vt = face.getUVs();
                int[] vn = face.getNormals();
                int[] vtan = face.getTangents();

                //vertex list
                List<Vertex> data = new ArrayList<>();

                for (int i = 0; i < v.length; i++) {
                    //parse indexes to their actual values
                    Vector3f a = vertices.get(v[i]);
                    Vector2f b = face.hasUVs() ? uvs.get(vt[i]) : Vertex.DEFAULT_UV;
                    Vector3f c = face.hasNormals() ? normals.get(vn[i]) : Vertex.DEFAULT_NORMAL;
                    Vector3f d = face.hasTangents() ? tangents.get(vtan[i]) : Vertex.DEFAULT_TANGENT;

                    //add to vertex list
                    data.add(new Vertex().pos(a).uv(b).normal(c).tangent(d));
                }

                //triangulate the faces using ear clipping
                List<Vertex> sorted = VertexHelper.triangulate(data);

                //add data to the vertex list
                sortedVertices.addAll(sorted);
            }

            //skip empty groups
            if (sortedVertices.isEmpty()) {
                LOGGER.debug("Skipping empty group");
                continue;
            }

            //default angle threshold for smoothing
            float angleThreshold = VertexHelper.DEFAULT_ANGLE_THRESHOLD;

            //generate normals when missing
            if (normals.isEmpty()) {
                LOGGER.debug("No normals data detected - generating normals");
                VertexHelper.calculateFlatNormals(sortedVertices);
                VertexHelper.smoothNormals(sortedVertices, angleThreshold);
            }

            //generate uvs when missing
            if (uvs.isEmpty()) {
                LOGGER.debug("No UV data detected - generating UVs");
                VertexHelper.calculateUVs(group.getBounds().getMin(), group.getBounds().getMax(), sortedVertices);
            }

            //calculate tangents
            if (tangents.isEmpty()) {
                LOGGER.debug("No tangents data detected - generating tangents");
                VertexHelper.calculateTangents(sortedVertices, angleThreshold);
            }

            //strip the unique indices from the vertex list
            Pair<int[], List<Vertex>> indices = VertexHelper.stripIndices(sortedVertices);

            //create a new group with the OpenGL attributes
            MeshData groupData = generateMesh(group.getBounds(), indices.second(), indices.first(), group.getMaterial());

            String groupName = group.getName();
            String newName = groupName;
            for (int i = 1; this.meshes.containsKey(newName); i++)
                newName = groupName + "_" + i;

            this.meshes.put(newName, groupData);
        }

        this.aabb.set(mesh.getBounds());
    }

    public Mesh getMesh() {
        return mesh;
    }

    @Override
    public Map<String, Material> getMaterials() {
        return mesh.getMaterials();
    }
}
