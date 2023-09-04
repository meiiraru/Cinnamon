package mayo.render;

import mayo.Client;
import mayo.model.Renderable;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

public class Batch {

    public static final int BATCH_SIZE = 2048;
    private static final int[] TEX_SLOTS = {0, 1, 2, 3, 4, 5, 6, 7};

    //vertex
    //pos                    tex id    uv              color      normal
    //float, float, float    float     float, float    r, g, b    float, float, float
    private static final int VERTEX_SIZE = 12;
    private static final int VERTEX_ELEMENTS = 5;
    private static final int STRIDE = VERTEX_SIZE * Float.BYTES;

    private final Renderable[] renderables;
    private final int[] offsets;
    private final List<Integer> textures;
    private final float[] vertices;
    private int size = 0;
    private int faceCount = 0;
    private boolean isFilled = false;
    private boolean reupload = false;

    private final Shader shader;
    private final int vaoID, vboID;

    public Batch(Shader shader) {
        this.shader = shader;
        this.renderables = new Renderable[BATCH_SIZE];
        this.offsets = new int[BATCH_SIZE + 1];
        this.textures = new ArrayList<>();
        //quad = 4 vertices
        this.vertices = new float[BATCH_SIZE * 4 * VERTEX_SIZE];

        //generate and bind a Vertex Array Object (VAO)
        this.vaoID = glGenVertexArrays();
        glBindVertexArray(vaoID);

        //allocate space for vertices, generating the Vertex Buffer Object (VBO)
        this.vboID = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboID);
        glBufferData(GL_ARRAY_BUFFER, (long) vertices.length * Float.BYTES, GL_DYNAMIC_DRAW);

        //create and upload indices buffer
        int eboID = glGenBuffers();
        int[] indices = generateIndices();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboID);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);

        //enable the buffer attribute pointers
        //pos - 3
        glVertexAttribPointer(0, 3, GL_FLOAT, false, STRIDE, 0);
        glEnableVertexAttribArray(0);

        //tex id - 1
        glVertexAttribPointer(1, 1, GL_FLOAT, false, STRIDE, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);

        //uv - 2
        glVertexAttribPointer(2, 2, GL_FLOAT, false, STRIDE, 4 * Float.BYTES);
        glEnableVertexAttribArray(2);

        //color - 3
        glVertexAttribPointer(3, 3, GL_FLOAT, false, STRIDE, 6 * Float.BYTES);
        glEnableVertexAttribArray(3);

        //normal - 3
        glVertexAttribPointer(4, 3, GL_FLOAT, false, STRIDE, 9 * Float.BYTES);
        glEnableVertexAttribArray(4);
    }

    public void render() {
        for (int i = 0; i < this.size; i++) {
            Renderable renderable = renderables[i];
            if (renderable.transform.isDirty()) {
                loadVertexProperties(i);
                reupload = true;
            }
        }

        if (reupload) {
            glBindBuffer(GL_ARRAY_BUFFER, vboID);
            glBufferSubData(GL_ARRAY_BUFFER, 0, vertices);
            reupload = false;
        }

        //use shader
        shader.use();
        shader.setMat4("projection", Client.getInstance().camera().getProjectionMatrix());
        shader.setMat4("view", Client.getInstance().camera().getViewMatrix());

        //textures
        for (int i = 0; i < textures.size(); i++) {
            glActiveTexture(GL_TEXTURE0 + i + 1);
            glBindTexture(GL_TEXTURE_2D, textures.get(i));
        }
        shader.setIntArray("textures", TEX_SLOTS);

        glBindVertexArray(vaoID);
        for (int i = 0; i < VERTEX_ELEMENTS; i++)
            glEnableVertexAttribArray(i);

        //pushed vertices * two triangles
        glDrawElements(GL_TRIANGLES, this.faceCount * 6, GL_UNSIGNED_INT, 0);

        for (int i = 0; i < VERTEX_ELEMENTS; i++)
            glDisableVertexAttribArray(i);

        glBindVertexArray(0);
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    public void addElement(Renderable renderable) {
        //get index and add the mesh
        int i = this.size;
        this.renderables[i] = renderable;
        this.size++;

        //add texture if it doesnt contains yet
        if (renderable.textureID > -1 && !hasTexture(renderable.textureID))
            textures.add(renderable.textureID);

        //face counting
        int faceCount = renderable.faceCount();
        this.faceCount += faceCount;

        //calculate vertex offset (a quad (4 vertices) per mesh)
        this.offsets[i + 1] = this.offsets[i] + (faceCount * 4 * VERTEX_SIZE);

        //add properties to local vertices array
        loadVertexProperties(i);

        //size checking
        this.isFilled = this.faceCount >= BATCH_SIZE;
    }

    public boolean removeElement(Renderable renderable) {
        for (int i = 0; i < this.size; i++) {
            if (renderables[i] == renderable) {
                int faceCount = renderable.faceCount();
                this.faceCount -= faceCount;

                int vertices = faceCount * 4 * VERTEX_SIZE;
                for (int j = i; j < this.size; j++) {
                    offsets[j] = offsets[j + 1] - vertices;
                }

                for (int j = i; j < this.size - 1; j++) {
                    renderables[j] = renderables[j + 1];
                    renderables[j].transform.dirty();
                }

                this.size--;
                return true;
            }
        }

        return false;
    }

    private void loadVertexProperties(int index) {
        Renderable renderable = this.renderables[index];

        //find textureID
        int texID = 0;
        if (renderable.textureID > -1) {
            for (int i = 0; i < textures.size(); i++) {
                if (textures.get(i) == renderable.textureID) {
                    texID = i + 1;
                    break;
                }
            }
        }

        //add vertices
        renderable.pushVertices(vertices, this.offsets[index], texID);
        renderable.transform.clean();
        reupload = true;
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

    public boolean isFilled() {
        return this.isFilled;
    }

    public boolean hasTextureSpace() {
        return this.textures.size() < 8;
    }

    public boolean hasTexture(int texture) {
        return this.textures.contains(texture);
    }
}
