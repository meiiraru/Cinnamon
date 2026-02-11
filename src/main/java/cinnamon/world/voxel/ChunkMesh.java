package cinnamon.world.voxel;

import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.*;

/**
 * GPU mesh for a single voxel chunk.
 * Holds VAO, VBO, EBO with the voxel vertex format:
 * position(3) + uv(2) + normal(3) + tangent(3) + texLayer(1) = 12 floats per vertex.
 * <p>
 * Uses GL_DYNAMIC_DRAW since chunks can be remeshed when blocks change.
 */
public class ChunkMesh {

    private static final int FLOAT_BYTES = Float.BYTES;
    private static final int VERTEX_FLOATS = ChunkMesher.VERTEX_FLOATS; // 12
    private static final int STRIDE = VERTEX_FLOATS * FLOAT_BYTES; // 36 bytes

    private int vao;
    private int vbo;
    private int ebo;
    private int indexCount;
    private int vertexCount;
    private boolean freed = false;

    /**
     * Create a ChunkMesh from raw vertex and index data.
     */
    public ChunkMesh(float[] vertices, int[] indices) {
        this.vertexCount = vertices.length / VERTEX_FLOATS;
        this.indexCount = indices.length;

        // Create VAO
        vao = glGenVertexArrays();
        glBindVertexArray(vao);

        // Create VBO
        vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);

        FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(vertices.length);
        vertexBuffer.put(vertices).flip();
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_DYNAMIC_DRAW);

        // Vertex attributes
        int offset = 0;

        // location 0: aPosition (vec3)
        glVertexAttribPointer(0, 3, GL_FLOAT, false, STRIDE, offset);
        glEnableVertexAttribArray(0);
        offset += 3 * FLOAT_BYTES;

        // location 1: aTexCoords (vec2)
        glVertexAttribPointer(1, 2, GL_FLOAT, false, STRIDE, offset);
        glEnableVertexAttribArray(1);
        offset += 2 * FLOAT_BYTES;

        // location 2: aNormal (vec3)
        glVertexAttribPointer(2, 3, GL_FLOAT, false, STRIDE, offset);
        glEnableVertexAttribArray(2);
        offset += 3 * FLOAT_BYTES;

        // location 3: aTangent (vec3)
        glVertexAttribPointer(3, 3, GL_FLOAT, false, STRIDE, offset);
        glEnableVertexAttribArray(3);
        offset += 3 * FLOAT_BYTES;

        // location 4: aTexLayer (float)
        glVertexAttribPointer(4, 1, GL_FLOAT, false, STRIDE, offset);
        glEnableVertexAttribArray(4);

        // Create EBO
        ebo = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);

        IntBuffer indexBuffer = BufferUtils.createIntBuffer(indexCount);
        indexBuffer.put(indices).flip();
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL_DYNAMIC_DRAW);

        glBindVertexArray(0);
    }

    /**
     * Re-upload mesh data (for remeshing).
     */
    public void update(float[] vertices, int[] indices) {
        this.vertexCount = vertices.length / VERTEX_FLOATS;
        this.indexCount = indices.length;

        glBindVertexArray(vao);

        // Update VBO
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(vertices.length);
        vertexBuffer.put(vertices).flip();
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_DYNAMIC_DRAW);

        // Update EBO
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        IntBuffer indexBuffer = BufferUtils.createIntBuffer(indexCount);
        indexBuffer.put(indices).flip();
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL_DYNAMIC_DRAW);

        glBindVertexArray(0);
    }

    /**
     * Bind and draw this chunk mesh. Caller is responsible for shader & uniform setup.
     */
    public void render() {
        if (freed || indexCount == 0) return;
        glBindVertexArray(vao);
        glDrawElements(GL_TRIANGLES, indexCount, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);
    }

    /**
     * Release GPU resources.
     */
    public void free() {
        if (freed) return;
        freed = true;
        glDeleteVertexArrays(vao);
        glDeleteBuffers(vbo);
        glDeleteBuffers(ebo);
    }

    public int getIndexCount() { return indexCount; }
    public int getVertexCount() { return vertexCount; }
    public boolean isFreed() { return freed; }
}
