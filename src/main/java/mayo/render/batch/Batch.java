package mayo.render.batch;

import mayo.model.Vertex;
import mayo.render.shader.Attributes;
import mayo.render.shader.Shader;
import mayo.render.shader.Shaders;
import mayo.utils.Pair;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import static mayo.render.shader.Attributes.getAttributes;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL15.GL_DYNAMIC_DRAW;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

public abstract class Batch { //vertex consumer

    //immutable properties
    protected static final int BUFFER_SIZE = 4098; //faces
    protected static final int[] TEXTURE_SLOTS = {0, 1, 2, 3, 4, 5, 6, 7};

    //buffer
    protected final List<Integer> textures;
    protected final FloatBuffer buffer;
    protected final int vertexSize;
    protected final int verticesPerFace;
    protected int faceCount = 0;

    //rendering data
    protected final Shader shader;
    protected final int vaoID, vboID;

    public Batch(Shaders shader, int verticesPerFace, int vertexFlags) {
        this.shader = shader.getShader();
        this.textures = new ArrayList<>();
        this.verticesPerFace = verticesPerFace;

        Pair<Integer, Integer> attributes = getAttributes(vertexFlags);
        this.vertexSize = attributes.second();
        //each face have 6 vertices, times the amount of vertex data
        int capacity = BUFFER_SIZE * verticesPerFace * vertexSize;
        buffer = BufferUtils.createFloatBuffer(capacity);

        //generate vao
        this.vaoID = glGenVertexArrays();
        glBindVertexArray(vaoID);

        //generate vbo
        this.vboID = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboID);
        glBufferData(GL_ARRAY_BUFFER, (long) capacity * Float.BYTES, GL_DYNAMIC_DRAW);

        //enable the shader attributes
        Attributes.load(vertexFlags, vertexSize);
        for (int i = 0; i < attributes.first(); i++)
            glEnableVertexAttribArray(i);
    }

    public void render(Matrix4f proj, Matrix4f view) {
        if (faceCount == 0)
            return;

        int count = buffer.position() / vertexSize;

        //bind buffer
        buffer.rewind();
        glBindBuffer(GL_ARRAY_BUFFER, vboID);
        glBufferSubData(GL_ARRAY_BUFFER, 0, buffer);

        //shader properties
        shader.use();
        shader.setProjectionMatrix(proj);
        shader.setViewMatrix(view);

        //textures
        for (int i = 0; i < textures.size(); i++) {
            glActiveTexture(GL_TEXTURE0 + i + 1);
            glBindTexture(GL_TEXTURE_2D, textures.get(i));
        }
        shader.setIntArray("textures", TEXTURE_SLOTS);

        //render
        glBindVertexArray(vaoID);
        glDrawArrays(GL_TRIANGLES, 0, count);

        //clear gl flags
        glBindVertexArray(0);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, 0);

        //clear buffers
        textures.clear();
        buffer.clear();
        faceCount = 0;
    }

    protected abstract void pushVertex(Vertex vertex, int textureID);

    public boolean pushFace(Vertex[] vertices, int textureID) {
        //cant add
        if (isFull())
            return false;

        //add texture
        if (textureID > 0 && !textures.contains(textureID))
            textures.add(textureID);
        int texID = textures.indexOf(textureID) + 1;

        //push the vertices
        for (int i = 1; i <= vertices.length - 2; i++) {
            pushVertex(vertices[0], texID);
            pushVertex(vertices[i], texID);
            pushVertex(vertices[i + 1], texID);
        }

        faceCount++;
        return true;
    }

    public boolean isFull() {
        //at least 1 face must be is free to push
        return buffer.remaining() < (vertexSize * verticesPerFace);
    }
}
