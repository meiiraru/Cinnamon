package mayo.model.obj;

import mayo.model.obj.material.Material;
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

    //materials
    private final Map<String, Material>
            materials = new HashMap<>();

    private boolean pbr;

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

    public Map<String, Material> getMaterials() {
        return materials;
    }

    public void setPBR(boolean bool) {
        this.pbr = bool;
    }

    public boolean isPBR() {
        return pbr;
    }
}
