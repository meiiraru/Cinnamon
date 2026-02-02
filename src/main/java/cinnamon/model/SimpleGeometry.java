package cinnamon.model;

import cinnamon.Client;
import cinnamon.render.shader.Attributes;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL30.*;

public class SimpleGeometry {

    protected final int vao, vbo;
    protected final int vertexCount;

    private SimpleGeometry(Vertex[] vertices, Attributes... attributes) {
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

    public static SimpleGeometry of(Vertex[] vertices, Attributes... attributes) {
        return new SimpleGeometry(vertices, attributes);
    }

    public static SimpleGeometry of(Vertex[] vertices, int[] indices, Attributes... attributes) {
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

    private static class EBOGeometry extends SimpleGeometry {
        protected final int ebo;
        protected final int indexCount;

        private EBOGeometry(Vertex[] vertices, int[] indices, Attributes... attributes) {
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


    public static final SimpleGeometry
            QUAD = of(unwarp(new Vertex[][]{GeometryHelper.invQuad(Client.getInstance().matrices, -1f, -1f, 2f, 2f)}),
                    Attributes.POS_XY, Attributes.UV),
            CUBE = of(unwarp(GeometryHelper.box(Client.getInstance().matrices, -1f, -1f, -1f, 1f, 1f, 1f, 0)),
                    Attributes.POS, Attributes.NORMAL),
            INV_CUBE = of(unwarp(GeometryHelper.box(Client.getInstance().matrices, 1f, 1f, 1f, -1f, -1f, -1f, 0)),
                    Attributes.POS, Attributes.NORMAL),
            SPHERE = of(unwarp(GeometryHelper.sphere(Client.getInstance().matrices, 0f, 0f, 0f, 1f, 12, 0)),
                    Attributes.POS, Attributes.NORMAL),
            CONE = of(unwarp(GeometryHelper.cone(Client.getInstance().matrices, 0, -1f, 0, 1f, 1f, 12, 0)),
                    Attributes.POS, Attributes.NORMAL);

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
