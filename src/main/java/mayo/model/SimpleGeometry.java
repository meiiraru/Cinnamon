package mayo.model;

import mayo.render.shader.Attributes;
import mayo.utils.Pair;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

public class SimpleGeometry {

    private final int vao;
    private final int renderMode;
    private final int vertexCount;

    public SimpleGeometry(Vertex[] vertices, int attributes, int renderMode) {
        this.renderMode = renderMode;
        this.vertexCount = vertices.length;

        //load vertex attributes
        Pair<Integer, Integer> pair = Attributes.getAttributes(attributes);
        int elements = pair.first();
        int vertexSize = pair.second();

        //prepare vertex buffer
        FloatBuffer buffer = BufferUtils.createFloatBuffer(vertices.length * vertexSize);
        for (Vertex vertex : vertices)
            Attributes.pushVertex(buffer, vertex, -1, attributes);
        buffer.rewind();

        //generate vao
        this.vao = glGenVertexArrays();
        glBindVertexArray(vao);

        //generate and bind vbo
        int vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);

        //enable the shader attributes
        Attributes.load(attributes, vertexSize);
        for (int i = 0; i < elements; i++)
            glEnableVertexAttribArray(i);

        //unbind vao
        glBindVertexArray(0);
    }

    public void render() {
        glBindVertexArray(vao);
        glDrawArrays(renderMode, 0, vertexCount);
    }

    public static SimpleGeometry quad() {
        return new SimpleGeometry(new Vertex[]{
                Vertex.of(-1f, -1f, 0f).uv(0f, 0f),
                Vertex.of(1f, -1f, 0f).uv(1f, 0f),
                Vertex.of(1f, 1f, 0f).uv(1f, 1f),
                Vertex.of(1f, 1f, 0f).uv(1f, 1f),
                Vertex.of(-1f, 1f, 0f).uv(0f, 1f),
                Vertex.of(-1f, -1f, 0f).uv(0f, 0f),
        }, Attributes.POS | Attributes.UV, GL_TRIANGLES);
    }

    public static SimpleGeometry invertedCube() {
        return new SimpleGeometry(new Vertex[]{
                //back
                Vertex.of(-1f, -1f, -1f),
                Vertex.of(1f, -1f, -1f),
                Vertex.of(1f, 1f, -1f),
                Vertex.of(1f, 1f, -1f),
                Vertex.of(-1f, 1f, -1f),
                Vertex.of(-1f, -1f, -1f),
                //front
                Vertex.of(-1f, -1f, 1f),
                Vertex.of(1f, 1f, 1f),
                Vertex.of(1f, -1f, 1f),
                Vertex.of(1f, 1f, 1f),
                Vertex.of(-1f, -1f, 1f),
                Vertex.of(-1f, 1f, 1f),
                //left
                Vertex.of(-1f, 1f, 1f),
                Vertex.of(-1f, -1f, -1f),
                Vertex.of(-1f, 1f, -1f),
                Vertex.of(-1f, -1f, -1f),
                Vertex.of(-1f, 1f, 1f),
                Vertex.of(-1f, -1f, 1f),
                //right
                Vertex.of(1f, 1f, 1f),
                Vertex.of(1f, 1f, -1f),
                Vertex.of(1f, -1f, -1f),
                Vertex.of(1f, -1f, -1f),
                Vertex.of(1f, -1f, 1f),
                Vertex.of(1f, 1f, 1f),
                //bottom
                Vertex.of(-1f, -1f, -1f),
                Vertex.of(1f, -1f, 1f),
                Vertex.of(1f, -1f, -1f),
                Vertex.of(1f, -1f, 1f),
                Vertex.of(-1f, -1f, -1f),
                Vertex.of(-1f, -1f, 1f),
                //top
                Vertex.of(-1f, 1f, -1f),
                Vertex.of(1f, 1f, -1f),
                Vertex.of(1f, 1f, 1f),
                Vertex.of(1f, 1f, 1f),
                Vertex.of(-1f, 1f, 1f),
                Vertex.of(-1f, 1f, -1f),
        }, Attributes.POS, GL_TRIANGLES);
    }
}
