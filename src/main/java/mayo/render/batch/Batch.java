package mayo.render.batch;

import mayo.Client;
import mayo.model.Vertex;
import mayo.render.shader.Attributes;
import mayo.render.shader.Shader;
import mayo.render.shader.Shaders;
import mayo.utils.Pair;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

public abstract class Batch { //vertex consumer

    //immutable properties
    protected static final int BUFFER_SIZE = 4098; //faces
    protected static final int[] TEXTURE_SLOTS = {0, 1, 2, 3, 4, 5, 6, 7};

    //buffer
    protected final List<Integer> textures;
    protected final FloatBuffer buffer;
    protected final int vertexSize;
    protected final int verticesPerFace;
    protected final int vertexFlags;
    protected int faceCount = 0;

    //rendering data
    protected final Shader shader;
    protected final int vaoID, vboID;

    public Batch(Shaders shader, int verticesPerFace, int vertexFlags) {
        this.shader = shader.getShader();
        this.textures = new ArrayList<>();
        this.verticesPerFace = verticesPerFace;
        this.vertexFlags = vertexFlags;

        Pair<Integer, Integer> attributes = Attributes.getAttributes(vertexFlags);
        this.vertexSize = attributes.second();
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
        Attributes.load(vertexFlags, vertexSize);
        for (int i = 0; i < attributes.first(); i++)
            glEnableVertexAttribArray(i);
    }

    public void render(Matrix4f proj, Matrix4f view) {
        if (faceCount == 0)
            return;

        int count = buffer.position() / vertexSize;

        //bind buffer
        buffer.rewind();
        glBindBuffer(GL_ARRAY_BUFFER, vboID);
        glBufferSubData(GL_ARRAY_BUFFER, 0, buffer);

        //shader properties
        shader.use().setup(proj, view);

        //function to run before drawing the vao
        preRender();

        //textures
        for (int i = 0; i < textures.size(); i++) {
            glActiveTexture(GL_TEXTURE0 + i);
            glBindTexture(GL_TEXTURE_2D, textures.get(i));
        }
        shader.setIntArray("textures", TEXTURE_SLOTS);

        //render
        glBindVertexArray(vaoID);
        glDrawArrays(primitive(), 0, count);

        //clear gl flags
        glBindVertexArray(0);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, 0);

        //clear buffers
        textures.clear();
        buffer.clear();
        faceCount = 0;
    }

    protected void preRender() {}

    protected int primitive() {
        return GL_TRIANGLES;
    }

    public boolean pushFace(Vertex[] vertices, int textureID) {
        //cant add
        if (isFull())
            return false;

        //add texture
        if (textureID != -1 && !textures.contains(textureID))
            textures.add(textureID);
        int texID = textures.indexOf(textureID);

        //unwrap and push the vertices
        unwrapVertices(vertices, texID);

        faceCount++;
        return true;
    }

    protected void unwrapVertices(Vertex[] vertices, int texID) {
        for (int i = 1; i <= vertices.length - 2; i++) {
            Attributes.pushVertex(buffer, vertices[0], texID, vertexFlags);
            Attributes.pushVertex(buffer, vertices[i], texID, vertexFlags);
            Attributes.pushVertex(buffer, vertices[i + 1], texID, vertexFlags);
        }
    }

    public boolean isFull() {
        //at least 1 face must be is free to push
        return buffer.remaining() < (vertexSize * verticesPerFace);
    }


    // -- children types -- //


    public static class FontFlatBatch extends Batch {
        public FontFlatBatch() {
            super(Shaders.FONT_FLAT, 6, Attributes.POS | Attributes.TEXTURE_ID | Attributes.UV | Attributes.COLOR_RGBA);
        }
    }

    public static class FontBatch extends Batch {
        public FontBatch() {
            super(Shaders.FONT, 6, Attributes.POS | Attributes.TEXTURE_ID | Attributes.UV | Attributes.COLOR_RGBA | Attributes.NORMAL);
        }

        @Override
        protected void preRender() {
            super.preRender();
            Client.getInstance().world.applyWorldUniforms(this.shader);
        }
    }

    public static class GUIBatch extends Batch {
        public GUIBatch() {
            super(Shaders.GUI, 6, Attributes.POS | Attributes.TEXTURE_ID | Attributes.UV | Attributes.COLOR_RGBA);
        }
    }

    public static class MainBatch extends Batch {
        public MainBatch() {
            super(Shaders.MAIN, 6, Attributes.POS | Attributes.TEXTURE_ID | Attributes.UV | Attributes.COLOR_RGBA | Attributes.NORMAL);
        }

        @Override
        protected void preRender() {
            super.preRender();
            Client.getInstance().world.applyWorldUniforms(this.shader);
        }
    }

    public static class LinesBatch extends Batch {
        public LinesBatch() {
            super(Shaders.LINES, 8, Attributes.POS | Attributes.COLOR_RGBA);
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
                Attributes.pushVertex(buffer, vertices[i - 1], texID, vertexFlags);
                Attributes.pushVertex(buffer, vertices[i], texID, vertexFlags);
            }

            //last pair
            Attributes.pushVertex(buffer, vertices[0], texID, vertexFlags);
            Attributes.pushVertex(buffer, vertices[len - 1], texID, vertexFlags);
        }
    }
}
