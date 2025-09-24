package cinnamon.render.model;

import cinnamon.model.Vertex;
import cinnamon.model.material.Material;
import cinnamon.render.shader.Attributes;
import cinnamon.utils.AABB;
import cinnamon.utils.Pair;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.util.List;

import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

public final class MeshData {
    private final int vao, vbo, vertexCount;
    private final Material material;
    private final AABB aabb = new AABB();

    public MeshData(AABB aabb, List<Vertex> vertices, Material material) {
        this.vertexCount = vertices.size();
        this.material = material;
        this.aabb.set(aabb);
        Pair<Integer, Integer> buffers = generateBuffers(vertices, Attributes.POS, Attributes.UV_FLIP, Attributes.NORMAL, Attributes.TANGENTS);
        this.vao = buffers.first();
        this.vbo = buffers.second();
    }
    private static Pair<Integer, Integer> generateBuffers(List<Vertex> vertices, Attributes... flags) {
        int vertexSize = Attributes.getVertexSize(flags);
        int capacity = vertices.size() * vertexSize;

        //vao
        int vao = glGenVertexArrays();
        glBindVertexArray(vao);

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

        return new Pair<>(vao, vbo);
    }

    public void render() {
        //bind vao
        glBindVertexArray(vao);

        //draw
        glDrawArrays(GL_TRIANGLES, 0, vertexCount);
    }

    public void free() {
        glDeleteBuffers(vao);
        glDeleteBuffers(vbo);
    }

    public AABB getAABB() {
        return aabb;
    }

    public Material getMaterial() {
        return material;
    }
}
