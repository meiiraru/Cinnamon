package cinnamon.render.model;

import cinnamon.model.Vertex;
import cinnamon.model.material.Material;
import cinnamon.render.MatrixStack;
import cinnamon.utils.AABB;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.util.Collection;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL31.glDrawElementsInstanced;
import static org.lwjgl.opengl.GL33.glVertexAttribDivisor;

public class InstancedMeshData extends MeshData {

    public static final int VEC4_SIZE_BYTES = 4 * Float.BYTES;
    public static final int MAT4_SIZE_BYTES = VEC4_SIZE_BYTES * 4;
    public static final int VEC3_SIZE_BYTES = 3 * Float.BYTES;
    public static final int MAT3_SIZE_BYTES = VEC3_SIZE_BYTES * 3;

    protected final int instanceVBO;
    protected int count;

    public InstancedMeshData(AABB aabb, Collection<Vertex> vertices, int[] indices, Material material) {
        super(aabb, vertices, indices, material);
        instanceVBO = genInstanceBuffer(attributeCount);
        attributeCount += 7; //4 attributes for the pos matrix and 3 for the normal matrix
    }

    private int genInstanceBuffer(int startLoaction) {
        //bind this vao
        glBindVertexArray(vao);

        //generate a new vbo
        int vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);

        int stride = MAT4_SIZE_BYTES + MAT3_SIZE_BYTES;
        int location = startLoaction;
        long offset = 0;

        //create and set attribute pointer for pos matrix (4 vec4)
        for (int i = 0; i < 4; i++) {
            //attribute index
            glEnableVertexAttribArray(location);
            glVertexAttribPointer(location, 4, GL_FLOAT, false, stride, offset);
            glVertexAttribDivisor(location, 1);

            //next attribute
            location++;
            offset += VEC4_SIZE_BYTES;
        }

        //create and set attribute pointer for normal matrix (3 vec3)
        for (int i = 0; i < 3; i++) {
            //attribute index
            glEnableVertexAttribArray(location);
            glVertexAttribPointer(location, 3, GL_FLOAT, false, stride, offset);
            glVertexAttribDivisor(location, 1);

            //next attribute
            location++;
            offset += VEC3_SIZE_BYTES;
        }

        glBindVertexArray(0);
        return vbo;
    }

    public void updateInstanceBuffer(Collection<MatrixStack.Matrices> matrices) {
        count = matrices.size();

        //prepare buffer
        int capacity = count * (16 + 9);
        FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(capacity);

        //extract matrices to the buffer
        for (MatrixStack.Matrices matrix : matrices) {
            //pos matrix (16 floats)
            matrix.pos().get(matrixBuffer);
            matrixBuffer.position(matrixBuffer.position() + 16);
            //normal matrix (9 floats)
            matrix.normal().get(matrixBuffer);
            matrixBuffer.position(matrixBuffer.position() + 9);
        }
        matrixBuffer.flip();

        //upload to the instance vbo
        glBindBuffer(GL_ARRAY_BUFFER, instanceVBO);
        glBufferData(GL_ARRAY_BUFFER, matrixBuffer, GL_STATIC_DRAW);
    }

    @Override
    public void render() {
        glBindVertexArray(vao);
        glDrawElementsInstanced(GL_TRIANGLES, indicesCount, GL_UNSIGNED_INT, 0, count);
        glBindVertexArray(0);
    }

    @Override
    public void free() {
        super.free();
        glDeleteBuffers(instanceVBO);
    }
}
