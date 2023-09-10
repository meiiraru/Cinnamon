package mayo.model;

import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class Mesh2 {

    //vertices data
    private final List<Vector3f>
            vertices = new ArrayList<>(),
            normals = new ArrayList<>();
    private final List<Vector2f>
            uvs = new ArrayList<>();

    //groups
    private final List<Group>
            groups = new ArrayList<>();

    //other properties
    private String mtllib;


    // -- loading and drawing -- //


    public void bake() {

    }

    public void render() {

    }


    // -- getters and setters -- //


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

    public void setMtllib(String mtllib) {
        this.mtllib = mtllib;
    }

    public String getMtllib() {
        return mtllib;
    }
}
