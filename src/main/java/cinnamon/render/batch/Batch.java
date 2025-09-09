package cinnamon.render.batch;

import cinnamon.model.Vertex;
import cinnamon.render.WorldRenderer;
import cinnamon.render.shader.Attributes;
import cinnamon.render.shader.Shader;
import cinnamon.render.texture.Texture;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL30.*;

public abstract class Batch { //vertex consumer

    //immutable properties
    protected static final int BUFFER_SIZE = 4098; //faces
    protected static final int[] TEXTURE_SLOTS;

    //buffer
    protected final List<Integer> textures;
    protected final FloatBuffer buffer;
    protected final int vertexSize;
    protected final int verticesPerFace;
    protected final Attributes[] attributes;
    protected int faceCount = 0;

    //rendering data
    protected final int vaoID, vboID;

    static {
        TEXTURE_SLOTS = new int[Texture.MAX_TEXTURES];
        for (int i = 0; i < TEXTURE_SLOTS.length; i++)
            TEXTURE_SLOTS[i] = i;
    }

    public Batch(int verticesPerFace, Attributes... attributes) {
        this.textures = new ArrayList<>();
        this.verticesPerFace = verticesPerFace;
        this.attributes = attributes;

        this.vertexSize = Attributes.getVertexSize(attributes);
        //each face have 6 vertices, times the amount of vertex data
        int capacity = BUFFER_SIZE * verticesPerFace * vertexSize;
        buffer = BufferUtils.createFloatBuffer(capacity);

        //generate vao
        this.vaoID = glGenVertexArrays();
        glBindVertexArray(vaoID);

        //generate vbo
        this.vboID = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboID);
        glBufferData(GL_ARRAY_BUFFER, (long) capacity * Float.BYTES, GL_DYNAMIC_DRAW);

        //enable the shader attributes
        Attributes.load(attributes, vertexSize);
        for (int i = 0; i < attributes.length; i++)
            glEnableVertexAttribArray(i);
    }

    public void free() {
        glDeleteBuffers(vboID);
        glDeleteVertexArrays(vaoID);
    }

    public int render(Shader shader) {
        if (!hasFace())
            return 0;

        int count = buffer.position() / vertexSize;

        //bind buffer
        buffer.rewind();
        glBindBuffer(GL_ARRAY_BUFFER, vboID);
        glBufferSubData(GL_ARRAY_BUFFER, 0, buffer);

        //function to run before drawing the vao
        preRender(shader);

        //textures
        for (int i = 0; i < textures.size(); i++)
            Texture.bind(textures.get(i), i);
        shader.setIntArray("textures", TEXTURE_SLOTS);

        //render
        glBindVertexArray(vaoID);
        glDrawArrays(primitive(), 0, count);

        //clear gl flags
        glBindVertexArray(0);
        Texture.unbindAll(textures.size() - 1);

        //clear buffers
        clear();
        return count;
    }

    public void clear() {
        if (faceCount > 0) {
            textures.clear();
            buffer.clear();
            faceCount = 0;
        }
    }

    protected void preRender(Shader shader) {}

    protected int primitive() {
        return GL_TRIANGLES;
    }

    public boolean pushFace(Vertex[] vertices, int textureID) {
        //cant add
        if (isFull(getUnwrappedVertexCount(vertices)))
            return false;

        //add texture
        if (textureID != -1 && !textures.contains(textureID)) {
            //cannot add texture
            if (textures.size() >= TEXTURE_SLOTS.length)
                return false;
            textures.add(textureID);
        }
        int texID = textures.indexOf(textureID);

        //unwrap and push the vertices
        unwrapVertices(vertices, texID);

        faceCount++;
        return true;
    }

    protected void unwrapVertices(Vertex[] vertices, int texID) {
        for (int i = 1; i <= vertices.length - 2; i++) {
            Attributes.pushVertex(buffer, vertices[0], texID, attributes);
            Attributes.pushVertex(buffer, vertices[i], texID, attributes);
            Attributes.pushVertex(buffer, vertices[i + 1], texID, attributes);
        }
    }

    protected int getUnwrappedVertexCount(Vertex[] vertices) {
        return (vertices.length - 2) * 3;
    }

    public boolean isFull(int size) {
        return buffer.remaining() < (vertexSize * size);
    }

    public boolean hasFace() {
        return faceCount > 0;
    }


    // -- children types -- //


    public static class MainFlatBatch extends Batch {
        public MainFlatBatch() {
            super(6, Attributes.POS, Attributes.TEXTURE_ID, Attributes.UV, Attributes.COLOR_RGBA);
        }
    }

    public static class MainBatch extends Batch {
        public MainBatch() {
            super(6, Attributes.POS, Attributes.TEXTURE_ID, Attributes.UV, Attributes.COLOR_RGBA, Attributes.NORMAL);
        }

        @Override
        protected void preRender(Shader shader) {
            super.preRender(shader);
            WorldRenderer.setSkyUniforms(shader);
        }
    }

    public static class LinesBatch extends Batch {
        public LinesBatch() {
            super(8, Attributes.POS, Attributes.COLOR_RGBA);
        }

        @Override
        protected int primitive() {
            return GL_LINES;
        }

        @Override
        protected void unwrapVertices(Vertex[] vertices, int texID) {
            int len = vertices.length;

            //loop through vertices
            for (int i = 1; i < len; i++) {
                Attributes.pushVertex(buffer, vertices[i - 1], texID, attributes);
                Attributes.pushVertex(buffer, vertices[i], texID, attributes);
            }

            //last pair
            Attributes.pushVertex(buffer, vertices[0], texID, attributes);
            Attributes.pushVertex(buffer, vertices[len - 1], texID, attributes);
        }

        @Override
        protected int getUnwrappedVertexCount(Vertex[] vertices) {
            return (vertices.length - 1) * 2 + 2;
        }
    }

    public static class ScreenSpaceUVBatch extends Batch {
        public ScreenSpaceUVBatch() {
            super(6, Attributes.POS, Attributes.TEXTURE_ID, Attributes.COLOR_RGBA, Attributes.NORMAL);
        }
    }
}
