package mayo.model;

import mayo.render.shader.Attributes;
import mayo.utils.Pair;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL30.*;

public class SimpleGeometry {

    protected final int vao, vbo;
    protected final int renderMode;
    protected final int vertexCount;

    private SimpleGeometry(Vertex[] vertices, int attributes, int renderMode) {
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
        this.vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);

        //enable the shader attributes
        Attributes.load(attributes, vertexSize);
        for (int i = 0; i < elements; i++)
            glEnableVertexAttribArray(i);

        //unbind vao
        glBindVertexArray(0);
    }

    public static SimpleGeometry of(Vertex[] vertices, int attributes, int renderMode) {
        return new SimpleGeometry(vertices, attributes, renderMode);
    }

    public static SimpleGeometry of(Vertex[] vertices, int attributes, int renderMode, int[] indices) {
        return new EBOGeometry(vertices, attributes, renderMode, indices);
    }

    public void render() {
        glBindVertexArray(vao);
        glDrawArrays(renderMode, 0, vertexCount);
    }

    public void free() {
        glDeleteVertexArrays(vao);
        glDeleteBuffers(vbo);
    }

    private static class EBOGeometry extends SimpleGeometry {
        protected final int ebo;
        protected final int indexCount;

        private EBOGeometry(Vertex[] vertices, int attributes, int renderMode, int[] indices) {
            super(vertices, attributes, renderMode);
            this.indexCount = indices.length;

            //rebind vao
            glBindVertexArray(vao);

            //generate ebo
            this.ebo = glGenBuffers();
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);

            //unbind vao
            glBindVertexArray(0);
        }

        @Override
        public void render() {
            glBindVertexArray(vao);
            glDrawElements(renderMode, indexCount, GL_UNSIGNED_INT, 0);
        }

        @Override
        public void free() {
            super.free();
            glDeleteBuffers(ebo);
        }
    }


    // -- generators -- //


    public static SimpleGeometry quad() {
        return of(new Vertex[]{
                Vertex.of(-1f, 1f).uv(0f, 1f),
                Vertex.of(-1f, -1f).uv(0f, 0f),
                Vertex.of(1f, -1f).uv(1f, 0f),
                Vertex.of(-1f, 1f).uv(0f, 1f),
                Vertex.of(1f, -1f).uv(1f, 0f),
                Vertex.of(1f, 1f).uv(1f, 1f),
        }, Attributes.POS_XY | Attributes.UV, GL_TRIANGLES);
    }

    public static SimpleGeometry invertedCube() {
        return of(new Vertex[]{
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

    public static SimpleGeometry sphere() {
        int xSegments = 64;
        int ySegments = 64;
        int vertexCount = (xSegments + 1) * (ySegments + 1);
        Vertex[] vertices = new Vertex[vertexCount];

        for (int y = 0; y <= ySegments; y++) {
            for (int x = 0; x <= xSegments; x++) {
                float xSegment = (float) x / xSegments;
                float ySegment = (float) y / ySegments;
                float xPos = (float) (Math.cos(xSegment * Math.PI * 2) * Math.sin(ySegment * Math.PI));
                float yPos = (float) Math.cos(ySegment * Math.PI);
                float zPos = (float) (Math.sin(xSegment * Math.PI * 2) * Math.sin(ySegment * Math.PI));
                vertices[x + y * (xSegments + 1)] = Vertex.of(xPos, yPos, zPos).uv(xSegment, ySegment).normal(xPos, yPos, zPos);
            }
        }

        int indexCount = ySegments * xSegments * 6;
        int[] indices = new int[indexCount];
        int i = 0;

        for (int y = 0; y < ySegments; y++) {
            for (int x = 0; x < xSegments; x++) {
                indices[i++] = (y + 1) * (xSegments + 1) + x;
                indices[i++] = y * (xSegments + 1) + x;
                indices[i++] = y * (xSegments + 1) + x + 1;
                indices[i++] = (y + 1) * (xSegments + 1) + x;
                indices[i++] = y * (xSegments + 1) + x + 1;
                indices[i++] = (y + 1) * (xSegments + 1) + x + 1;
            }
        }

        return of(vertices, Attributes.POS | Attributes.UV | Attributes.NORMAL, GL_TRIANGLES, indices);
    }
}
