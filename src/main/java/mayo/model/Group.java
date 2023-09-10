package mayo.model;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

public class Group {

    //properties
    private final String name;
    private final List<Face> faces = new ArrayList<>();

    private String material;
    private boolean smooth;

    //rendering
    private int vao, vertexCount;

    public Group(String name) {
        this.name = name;
    }

    public boolean isEmpty() {
        return faces.isEmpty();
    }


    // -- rendering functions -- //


    public int generateBuffers() {
        //vertices data
        int capacity = 0;
        for (Face face : faces) {
            capacity += face.getLength();
            vertexCount += face.getVertexCount();
        }

        //vao
        this.vao = glGenVertexArrays();
        glBindVertexArray(vao);

        //vbo
        int vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, (long) capacity * Float.BYTES, GL_STATIC_DRAW);

        //return the buffer size
        return capacity;
    }

    public void render() {
        glBindVertexArray(vao);
        glDrawArrays(GL_TRIANGLES, 0, vertexCount);
    }


    // -- getters and setters -- //


    public String getName() {
        return name;
    }

    public List<Face> getFaces() {
        return faces;
    }

    public String getMaterial() {
        return material;
    }

    public void setMaterial(String material) {
        this.material = material;
    }

    public boolean isSmooth() {
        return smooth;
    }

    public void setSmooth(boolean smooth) {
        this.smooth = smooth;
    }
}
