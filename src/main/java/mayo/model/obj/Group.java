package mayo.model.obj;

import mayo.render.shader.Attributes;
import mayo.utils.Pair;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glDisableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
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
    private Pair<Integer, Integer> vertexAttributes;

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
        this.vertexAttributes = Attributes.getAttributes(flags);

        //vao
        this.vao = glGenVertexArrays();
        glBindVertexArray(vao);

        //vbo
        int vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, (long) capacity * Float.BYTES, GL_STATIC_DRAW);

        //load vertex attributes
        Attributes.load(flags, vertexAttributes.second());

        //return the generated buffer
        return BufferUtils.createFloatBuffer(capacity);
    }

    public void render() {
        //enable attributes
        int len = vertexAttributes.first();
        for (int i = 0; i < len; i++)
            glEnableVertexAttribArray(i);

        //draw
        glBindVertexArray(vao);
        glDrawArrays(GL_TRIANGLES, 0, vertexCount);

        //disable attributes
        for (int i = 0; i < len; i++)
            glDisableVertexAttribArray(i);
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
