package cinnamon.model;

import cinnamon.render.shader.VertexAttribute;
import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static cinnamon.render.shader.VertexAttribute.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

public class StaticGeometry {

    protected final int vao, vbo;
    protected final int vertexCount;

    private StaticGeometry(Vertex[] vertices, VertexAttribute... attributes) {
        this.vertexCount = vertices.length;

        //load vertex attributes
        int elements = attributes.length;
        int vertexSize = VertexAttribute.getStrideInBytes(attributes);

        //prepare vertex buffer
        ByteBuffer buffer = BufferUtils.createByteBuffer(vertices.length * vertexSize);
        for (Vertex vertex : vertices)
            VertexAttribute.pushVertex(buffer, vertex, -1, attributes);
        buffer.rewind();

        //generate vao
        this.vao = glGenVertexArrays();
        glBindVertexArray(vao);

        //generate and bind vbo
        this.vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);

        //enable the shader attributes
        VertexAttribute.load(attributes);
        for (int i = 0; i < elements; i++)
            glEnableVertexAttribArray(i);

        //unbind vao
        glBindVertexArray(0);
    }

    public static StaticGeometry of(Vertex[] vertices, VertexAttribute... attributes) {
        return new StaticGeometry(vertices, attributes);
    }

    public static StaticGeometry of(Vertex[][] vertices, VertexAttribute... attributes) {
        return of(unwarp(vertices), attributes);
    }

    public static StaticGeometry of(Vertex[] vertices, int[] indices, VertexAttribute... attributes) {
        return new EBOGeometry(vertices, indices, attributes);
    }

    public void render() {
        glBindVertexArray(vao);
        glDrawArrays(GL_TRIANGLES, 0, vertexCount);
        glBindVertexArray(0);
    }

    public void free() {
        glDeleteVertexArrays(vao);
        glDeleteBuffers(vbo);
    }

    private static class EBOGeometry extends StaticGeometry {
        protected final int ebo;
        protected final int indexCount;

        private EBOGeometry(Vertex[] vertices, int[] indices, VertexAttribute... attributes) {
            super(vertices, attributes);
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
            glDrawElements(GL_TRIANGLES, indexCount, GL_UNSIGNED_INT, 0);
            glBindVertexArray(0);
        }

        @Override
        public void free() {
            super.free();
            glDeleteBuffers(ebo);
        }
    }


    // -- generators -- //


    public static final StaticGeometry
            QUAD = of(new Vertex[][]{GeometryHelper.invRectangle(null, -1f, -1f, 1f, 1f, 0f, 0xFFFFFFFF)},
                    POS_XY, UV, NORMAL),
            TRIANGLE = of(new Vertex[][]{GeometryHelper.invTriangle(null, -1f, -1f, 1f, 1f, 0f, 0xFFFFFFFF)},
                    POS_XY, UV, NORMAL),
            CUBE = of(GeometryHelper.box(null, -1f, -1f, -1f, 1f, 1f, 1f, 0),
                    POS, UV, NORMAL),
            INV_CUBE = of(GeometryHelper.box(null, 1f, 1f, 1f, -1f, -1f, -1f, 0),
                    POS, UV, NORMAL),
            SPHERE = of(GeometryHelper.sphere(null, 0f, 0f, 0f, 1f, 12, 0),
                    POS, UV, NORMAL),
            CONE = of(GeometryHelper.cone(null, 0, -1f, 0, 1f, 1f, 12, 0),
                    POS, UV, NORMAL);

    private static List<Vertex> unwarp(Vertex[] vertices) {
        List<Vertex> result = new ArrayList<>();
        for (int i = 1; i <= vertices.length - 2; i++) {
            result.add(vertices[0]);
            result.add(vertices[i]);
            result.add(vertices[i + 1]);
        }
        return result;
    }

    private static Vertex[] unwarp(Vertex[][] faces) {
        List<Vertex> result = new ArrayList<>();
        for (Vertex[] face : faces)
            result.addAll(unwarp(face));
        return result.toArray(new Vertex[0]);
    }
}
