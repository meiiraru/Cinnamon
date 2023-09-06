package mayo.render;

import mayo.model.Renderable;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

public class Batch {

    public static final int BATCH_SIZE = 2048;
    private static final int[] TEX_SLOTS = {0, 1, 2, 3, 4, 5, 6, 7};

    private final List<Integer> textures;
    private final FloatBuffer vertices;
    private int faceCount = 0;
    private boolean isFull = false;

    private final Shader shader;
    private final int vaoID, vboID;

    public Batch(Shader shader) {
        this.shader = shader;
        this.textures = new ArrayList<>();
        //quad = 4 vertices
        int capacity = BATCH_SIZE * 4 * shader.vertexSize;
        this.vertices = BufferUtils.createFloatBuffer(capacity);

        //generate and bind a Vertex Array Object (VAO)
        this.vaoID = glGenVertexArrays();
        glBindVertexArray(vaoID);

        //allocate space for vertices, generating the Vertex Buffer Object (VBO)
        this.vboID = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboID);
        glBufferData(GL_ARRAY_BUFFER, (long) capacity * Float.BYTES, GL_DYNAMIC_DRAW);

        //create and upload indices buffer
        int eboID = glGenBuffers();
        int[] indices = generateIndices();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboID);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);

        //enable the shader attribute pointers
        shader.loadAttributes();
    }

    public void render() {
        vertices.rewind();
        glBindBuffer(GL_ARRAY_BUFFER, vboID);
        glBufferSubData(GL_ARRAY_BUFFER, 0, vertices);

        //use shader
        shader.use();

        //textures
        for (int i = 0; i < textures.size(); i++) {
            glActiveTexture(GL_TEXTURE0 + i + 1);
            glBindTexture(GL_TEXTURE_2D, textures.get(i));
        }
        shader.setIntArray("textures", TEX_SLOTS);

        glBindVertexArray(vaoID);
        for (int i = 0; i < shader.elements; i++)
            glEnableVertexAttribArray(i);

        //pushed vertices * two triangles
        glDrawElements(GL_TRIANGLES, this.faceCount * 6, GL_UNSIGNED_INT, 0);

        for (int i = 0; i < shader.elements; i++)
            glDisableVertexAttribArray(i);

        glBindVertexArray(0);
        glBindTexture(GL_TEXTURE_2D, 0);

        //finish renderer
        textures.clear();
        vertices.clear();
        faceCount = 0;
        isFull = false;
    }

    public boolean addElement(MatrixStack matrices, Renderable renderable) {
        if (isFull() || !hasSpace(renderable.faceCount()) || (renderable.textureID > -1 && !hasTextureSpace() && !hasTexture(renderable.textureID)))
            return false;

        //add texture if it doesnt contains yet
        if (renderable.textureID > -1 && !hasTexture(renderable.textureID))
            textures.add(renderable.textureID);

        //face counting
        this.faceCount += renderable.faceCount();

        //add properties to local vertices array
        //find textureID
        int texID = textures.indexOf(renderable.textureID) + 1;

        //add vertices
        renderable.pushVertices(matrices, vertices, texID);

        //size checking
        this.isFull = this.faceCount >= BATCH_SIZE;

        return true;
    }

    private int[] generateIndices() {
        //6 indices per quad (3 per triangle)
        int[] elements = new int[6 * BATCH_SIZE];
        for (int i = 0; i < BATCH_SIZE; i++)
            loadElementIndices(elements, i);

        return elements;
    }

    private void loadElementIndices(int[] elements, int index) {
        int offsetArrayIndex = 6 * index; //two triangles
        int offset = 4 * index; //a quad

        //3, 2, 0, 0, 2, 1    7, 6, 4, 4, 6, 5    ...
        elements[offsetArrayIndex]     = offset + 3;
        elements[offsetArrayIndex + 1] = offset + 2;
        elements[offsetArrayIndex + 2] = offset;

        elements[offsetArrayIndex + 3] = offset;
        elements[offsetArrayIndex + 4] = offset + 2;
        elements[offsetArrayIndex + 5] = offset + 1;
    }

    public boolean isFull() {
        return this.isFull;
    }

    public boolean hasSpace(int faceCount) {
        return this.vertices.remaining() >= faceCount * shader.vertexSize;
    }

    public boolean hasTextureSpace() {
        return this.textures.size() < 7;
    }

    public boolean hasTexture(int texture) {
        return this.textures.contains(texture);
    }
}
