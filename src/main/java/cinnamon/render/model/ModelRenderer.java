package cinnamon.render.model;

import cinnamon.model.Vertex;
import cinnamon.model.material.Material;
import cinnamon.render.MatrixStack;
import cinnamon.render.shader.Attributes;
import cinnamon.utils.AABB;
import cinnamon.utils.Pair;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.util.List;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

public abstract class ModelRenderer {

    public abstract void free();

    public abstract void render(MatrixStack matrices);

    public abstract void render(MatrixStack matrices, Material material);

    public abstract void renderWithoutMaterial(MatrixStack matrices);

    public abstract AABB getAABB();

    public abstract List<AABB> getPreciseAABB();

    protected static Pair<Integer, Integer> generateBuffers(List<Vertex> vertices, Attributes... flags) {
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
}
