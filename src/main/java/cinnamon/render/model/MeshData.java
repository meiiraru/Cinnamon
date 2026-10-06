package cinnamon.render.model;

import cinnamon.math.collision.shape.AABB;
import cinnamon.model.Vertex;
import cinnamon.model.material.Material;
import cinnamon.render.shader.VertexAttribute;
import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;
import java.util.Collection;

import static cinnamon.render.shader.VertexAttribute.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

public class MeshData {

    public static final VertexAttribute[] DEFAULT_ATTRIBUTES = {POS, UV_FLIP, NORMAL, TANGENTS};

    protected final int vao, vbo, ebo, indicesCount;
    protected int attributeCount;
    private final Material material;
    private final AABB aabb = new AABB();

    public MeshData(AABB aabb, Collection<Vertex> vertices, int[] indices, Material material) {
        this.indicesCount = indices.length;
        this.material = material;
        this.aabb.set(aabb);

        //vao
        this.vao = glGenVertexArrays();
        glBindVertexArray(vao);

        //vbo
        this.attributeCount = DEFAULT_ATTRIBUTES.length;
        this.vbo = generateVertexBuffer(vertices, DEFAULT_ATTRIBUTES);

        //ebo
        this.ebo = generateIndices(indices);

        glBindVertexArray(0);
    }

    protected static int generateVertexBuffer(Collection<Vertex> vertices, VertexAttribute... flags) {
        int vertexSize = VertexAttribute.getStrideInBytes(flags);
        int capacity = vertices.size() * vertexSize;

        //vbo
        int vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, capacity, GL_STATIC_DRAW);

        //load vertex attributes
        VertexAttribute.load(flags);

        //enable attributes
        for (int i = 0; i < flags.length; i++)
            glEnableVertexAttribArray(i);

        //different buffer per group
        ByteBuffer buffer = BufferUtils.createByteBuffer(capacity);

        //push vertices to buffer
        for (Vertex vertex : vertices)
            VertexAttribute.pushVertex(buffer, vertex, 0, flags);

        //bind buffer to the current VBO
        buffer.rewind();
        glBufferSubData(GL_ARRAY_BUFFER, 0, buffer);

        return vbo;
    }

    protected static int generateIndices(int[] indices) {
        int ebo = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);
        return ebo;
    }

    public void render() {
        glBindVertexArray(vao);
        glDrawElements(GL_TRIANGLES, indicesCount, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);
    }

    public void free() {
        glDeleteVertexArrays(vao);
        glDeleteBuffers(vbo);
        glDeleteBuffers(ebo);
    }

    public AABB getAABB() {
        return aabb;
    }

    public Material getMaterial() {
        return material;
    }
}
