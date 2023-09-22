package mayo.model.obj;

import org.joml.Vector2f;
import org.joml.Vector3f;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.glBufferSubData;

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
            bbMin = new Vector3f(),
            bbMax = new Vector3f();

    //materials
    private final Map<String, Material>
            materials = new HashMap<>();


    // -- loading and drawing -- //


    public Mesh bake() {
        for (Group group : groups) {
            FloatBuffer buffer = group.generateBuffers();

            for (Face face : group.getFaces()) {
                List<Integer> v = face.getVertices();
                List<Integer> vt = face.getUVs();
                List<Integer> vn = face.getNormals();

                for (int i = 0; i < v.size(); i++) {
                    fillBuffer(buffer, vertices.get(v.get(i)));
                    if (!vt.isEmpty())
                        fillBuffer(buffer, uvs.get(vt.get(i)));
                    if (!vn.isEmpty())
                        fillBuffer(buffer, normals.get(vn.get(i)));
                }
            }

            buffer.rewind();
            glBufferSubData(GL_ARRAY_BUFFER, 0, buffer);
        }

        return this;
    }

    private static void fillBuffer(FloatBuffer buffer, Vector2f vec) {
        buffer.put(vec.x);
        buffer.put(vec.y);
    }

    private static void fillBuffer(FloatBuffer buffer, Vector3f vec) {
        buffer.put(vec.x);
        buffer.put(vec.y);
        buffer.put(vec.z);
    }

    public void render() {
        for (Group group : groups)
            group.render();
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

    public Vector3f getBBMin() {
        return bbMin;
    }

    public Vector3f getBBMax() {
        return bbMax;
    }

    public Vector3f getBoundingBox() {
        return getBBMax().sub(getBBMin(), new Vector3f());
    }

    public Map<String, Material> getMaterials() {
        return materials;
    }
}
