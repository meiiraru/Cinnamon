package mayo.model.obj;

import mayo.utils.AABB;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Mesh {

    //vertices data
    private final List<Vector3f>
            vertices = new ArrayList<>(),
            normals = new ArrayList<>();
    private final List<Vector2f>
            uvs = new ArrayList<>();

    //groups
    private final List<Group>
            groups = new ArrayList<>();

    //bounding box
    private final Vector3f
            bbMin = new Vector3f(Integer.MAX_VALUE),
            bbMax = new Vector3f(Integer.MIN_VALUE);

    //materials
    private final Map<String, Material>
            materials = new HashMap<>();

    public List<Vector3f> getVertices() {
        return vertices;
    }

    public List<Vector3f> getNormals() {
        return normals;
    }

    public List<Vector2f> getUVs() {
        return uvs;
    }

    public List<Group> getGroups() {
        return groups;
    }

    public Vector3f getBBMin() {
        return bbMin;
    }

    public Vector3f getBBMax() {
        return bbMax;
    }

    public AABB getAABB() {
        return new AABB(bbMin, bbMax);
    }

    public Map<String, Material> getMaterials() {
        return materials;
    }
}
