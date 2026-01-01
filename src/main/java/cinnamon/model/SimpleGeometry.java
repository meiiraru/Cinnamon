package cinnamon.model;

import cinnamon.render.shader.Attributes;
import org.joml.Math;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL30.*;

public class SimpleGeometry {

    protected final int vao, vbo;
    protected final int renderMode;
    protected final int vertexCount;

    private SimpleGeometry(Vertex[] vertices, int renderMode, Attributes... attributes) {
        this.renderMode = renderMode;
        this.vertexCount = vertices.length;

        //load vertex attributes
        int elements = attributes.length;
        int vertexSize = Attributes.getVertexSize(attributes);

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

    public static SimpleGeometry of(Vertex[] vertices, int renderMode, Attributes... attributes) {
        return new SimpleGeometry(vertices, renderMode, attributes);
    }

    public static SimpleGeometry of(Vertex[] vertices, int renderMode, int[] indices, Attributes... attributes) {
        return new EBOGeometry(vertices, renderMode, indices, attributes);
    }

    public void render() {
        glBindVertexArray(vao);
        glDrawArrays(renderMode, 0, vertexCount);
        glBindVertexArray(0);
    }

    public void free() {
        glDeleteVertexArrays(vao);
        glDeleteBuffers(vbo);
    }

    private static class EBOGeometry extends SimpleGeometry {
        protected final int ebo;
        protected final int indexCount;

        private EBOGeometry(Vertex[] vertices, int renderMode, int[] indices, Attributes... attributes) {
            super(vertices, renderMode, attributes);
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
            glBindVertexArray(0);
        }

        @Override
        public void free() {
            super.free();
            glDeleteBuffers(ebo);
        }
    }


    // -- generators -- //


    public static final SimpleGeometry
            QUAD = quad(), //CCW
            INVERTED_CUBE = invertedCube(),
            SPHERE = sphere(),
            CONE = cone();

    private static SimpleGeometry quad() {
        return of(new Vertex[]{
                Vertex.of(-1f, 1f).uv(0f, 1f),
                Vertex.of(-1f, -1f).uv(0f, 0f),
                Vertex.of(1f, -1f).uv(1f, 0f),
                Vertex.of(-1f, 1f).uv(0f, 1f),
                Vertex.of(1f, -1f).uv(1f, 0f),
                Vertex.of(1f, 1f).uv(1f, 1f),
        }, GL_TRIANGLES, Attributes.POS_XY, Attributes.UV);
    }

    private static SimpleGeometry invertedCube() {
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
        }, GL_TRIANGLES, Attributes.POS);
    }

    private static SimpleGeometry sphere() {
        int xSegments = 12;
        int ySegments = 12;
        int vertexCount = (xSegments + 1) * (ySegments + 1);
        Vertex[] vertices = new Vertex[vertexCount];

        for (int y = 0; y <= ySegments; y++) {
            for (int x = 0; x <= xSegments; x++) {
                float xSegment = (float) x / xSegments;
                float ySegment = (float) y / ySegments;
                float xPos = Math.cos(xSegment * Math.PI_TIMES_2_f) * Math.sin(ySegment * Math.PI_f);
                float yPos = Math.cos(ySegment * Math.PI_f);
                float zPos = Math.sin(xSegment * Math.PI_TIMES_2_f) * Math.sin(ySegment * Math.PI_f);
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

        return of(vertices, GL_TRIANGLES, indices, Attributes.POS, Attributes.UV, Attributes.NORMAL);
    }

    private static SimpleGeometry cone() {
        int segments = 12;
        int vertexCount = segments + 2;
        Vertex[] vertices = new Vertex[vertexCount];

        //tip and base
        vertices[0] = Vertex.of(0f, 0f, 0f);
        vertices[1] = Vertex.of(0f, -1f, 0f);

        //sides
        float step = Math.PI_TIMES_2_f / segments;
        for (int i = 0; i < segments; i++) {
            float angle = i * step;
            float x = Math.cos(angle);
            float z = Math.sin(angle);
            vertices[i + 2] = Vertex.of(x, -1f, z);
        }

        int indexCount = segments * 6;
        int[] indices = new int[indexCount];
        int i = 0;

        for (int j = 0; j < segments; j++) {
            //side
            indices[i++] = 0;
            indices[i++] = ((j + 1) % segments) + 2;
            indices[i++] = j + 2;

            //base
            indices[i++] = 1;
            indices[i++] = j + 2;
            indices[i++] = ((j + 1) % segments) + 2;
        }

        return of(vertices, GL_TRIANGLES, indices, Attributes.POS);
    }
}
