package mayo.model.obj;

import mayo.render.shader.Attributes;
import mayo.utils.Pair;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

public class Group {

    //properties
    private final String name;
    private final List<Face> faces = new ArrayList<>();

    private Material material;
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


    public FloatBuffer generateBuffers() {
        //vertices data
        int capacity = 0;
        for (Face face : faces) {
            capacity += face.getLength();
            vertexCount += face.getVertexCount();
        }

        int flags = Attributes.POS | Attributes.UV | Attributes.NORMAL;
        Pair<Integer, Integer> attrib = Attributes.getAttributes(flags);

        //vao
        this.vao = glGenVertexArrays();
        glBindVertexArray(vao);

        //vbo
        int vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, (long) capacity * Float.BYTES, GL_STATIC_DRAW);

        //load vertex attributes
        Attributes.load(flags, attrib.second());

        //enable attributes
        for (int i = 0; i < attrib.first(); i++)
            glEnableVertexAttribArray(i);

        //return the generated buffer
        return BufferUtils.createFloatBuffer(capacity);
    }

    public void render() {
        //bind vao
        glBindVertexArray(vao);

        //bind material
        if (material != null) {
            material.use();
        } else {
            glBindTexture(GL_TEXTURE_2D, 0);
        }

        //draw
        glDrawArrays(GL_TRIANGLES, 0, vertexCount);
    }


    // -- getters and setters -- //


    public String getName() {
        return name;
    }

    public List<Face> getFaces() {
        return faces;
    }

    public Material getMaterial() {
        return material;
    }

    public void setMaterial(Material material) {
        this.material = material;
    }

    public boolean isSmooth() {
        return smooth;
    }

    public void setSmooth(boolean smooth) {
        this.smooth = smooth;
    }
}
