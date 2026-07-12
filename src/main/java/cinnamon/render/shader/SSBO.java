package cinnamon.render.shader;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL30.glBindBufferBase;
import static org.lwjgl.opengl.GL42.glMemoryBarrier;
import static org.lwjgl.opengl.GL43.GL_SHADER_STORAGE_BARRIER_BIT;
import static org.lwjgl.opengl.GL43.GL_SHADER_STORAGE_BUFFER;

/**
 * a Shader Storage Buffer Object (SSBO) is a buffer that can be read from and written to by shaders
 * <br>
 * <br>
 * it can be used to store large amounts of data that can be accessed by the GPU, such as particle systems,
 * physics simulations, or any other data that needs to be shared between the CPU and GPU
 */
public class SSBO {

    private final int id;

    /**
     * creates a new SSBO and allocates an ID for it
     */
    public SSBO() {
        this.id = glGenBuffers();
    }

    /**
     * @return the ID of this SSBO
     */
    public int getId() {
        return id;
    }

    /**
     * binds the SSBO of the given ID to the given index in the shader
     * @param id the ID of the SSBO to bind
     * @param index the index in the shader to bind the SSBO to
     */
    public static void bind(int id, int index) {
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, index, id);
    }

    /**
     * binds this SSBO to the given index in the shader
     * @param index the index in the shader to bind the SSBO to
     */
    public void bind(int index) {
        bind(getId(), index);
    }

    /**
     * binds this SSBO to the current shader context
     */
    public void bind() {
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, id);
    }

    /**
     * unbinds the currently bound SSBO
     */
    public static void unbind() {
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);
    }

    /**
     * frees the buffer from GPU memory
     */
    public void free() {
        glDeleteBuffers(id);
    }

    /**
     * allocates empty memory for the buffer
     */
    public void allocate(long size, int usage) {
        bind();
        glBufferData(GL_SHADER_STORAGE_BUFFER, size * Float.BYTES, usage);
    }

    /**
     * uploads initial data to the buffer
     */
    public void uploadData(float[] data, int usage) {
        bind();
        glBufferData(GL_SHADER_STORAGE_BUFFER, data, usage);
    }

    /**
     * ensures that any writes to the SSBO from a previous shader invocation are visible to the next ones
     */
    public static void memoryBarrier() {
        glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT);
    }
}