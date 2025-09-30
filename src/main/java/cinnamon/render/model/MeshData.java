package cinnamon.render.model;

import cinnamon.model.Vertex;
import cinnamon.model.material.Material;
import cinnamon.render.shader.Attributes;
import cinnamon.utils.AABB;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.util.Collection;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL30.*;

public class MeshData {

    public static final Attributes[] DEFAULT_ATTRIBUTES = {Attributes.POS, Attributes.UV_FLIP, Attributes.NORMAL, Attributes.TANGENTS};

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

    protected static int generateVertexBuffer(Collection<Vertex> vertices, Attributes... flags) {
        int vertexSize = Attributes.getVertexSize(flags);
        int capacity = vertices.size() * vertexSize;

        //vbo
        int vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, (long) capacity * Float.BYTES, GL_STATIC_DRAW);

        //load vertex attributes
        Attributes.load(flags, vertexSize);

        //enable attributes
        for (int i = 0; i < flags.length; i++)
            glEnableVertexAttribArray(i);

        //different buffer per group
        FloatBuffer buffer = BufferUtils.createFloatBuffer(capacity);

        //push vertices to buffer
        for (Vertex vertex : vertices)
            Attributes.pushVertex(buffer, vertex, 0, flags);

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
