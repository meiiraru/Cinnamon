package cinnamon.render.shader;

import cinnamon.model.Vertex;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.nio.FloatBuffer;
import java.util.function.BiConsumer;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;

public enum Attributes {

    /**
     * Vertex Attributes
     * <table><tr>
     * <td>POS {@link Vector3f Vec3}</td><td>POS_XY {@link Vector2f Vec2}</td><td>TEXTURE_ID {@link Integer int}</td><td>UV {@link Vector2f Vec2}</td><td>COLOR {@link Vector3f Vec3}</td>
     * <td>COLOR_RGBA {@link Vector4f Vec4}</td><td>NORMAL {@link Vector3f Vec3}</td><td>INDEX {@link Integer int}</td><td>TANGENTS {@link Vector3f Vec3}</td>
     * </tr></table>
    **/
    POS(3, (v, b) -> {
        Vector3f pos = v.getPosition();
        b.put(pos.x);
        b.put(pos.y);
        b.put(pos.z);
    }),
    POS_XY(2, (v, b) -> {
        Vector3f pos = v.getPosition();
        b.put(pos.x);
        b.put(pos.y);
    }),
    TEXTURE_ID(1, null, true),
    UV(2, (v, b) -> {
        Vector2f uv = v.getUV();
        b.put(uv.x);
        b.put(uv.y);
    }),
    COLOR(3, (v, b) -> {
        Vector4f color = v.getColor();
        b.put(color.x);
        b.put(color.y);
        b.put(color.z);
    }),
    COLOR_RGBA(4, (v, b) -> {
        Vector4f color = v.getColor();
        b.put(color.x);
        b.put(color.y);
        b.put(color.z);
        b.put(color.w);
    }),
    NORMAL(3, (v, b) -> {
        Vector3f normal = v.getNormal();
        b.put(normal.x);
        b.put(normal.y);
        b.put(normal.z);
    }),
    INDEX(1, (v, b) -> b.put(v.getIndex()), true),
    TANGENTS(3, null);

    private final int size;
    private final int sizeInBytes;
    private final BiConsumer<Vertex, FloatBuffer> consumer;
    private final boolean normalized;

    Attributes(int size, BiConsumer<Vertex, FloatBuffer> consumer) {
        this(size, consumer, false);
    }

    Attributes(int size, BiConsumer<Vertex, FloatBuffer> consumer, boolean normalized) {
        this.size = size;
        this.sizeInBytes = size * Float.BYTES;
        this.consumer = consumer;
        this.normalized = normalized;
    }

    public static int getVertexSize(Attributes... flags) {
        int verts = 0;
        for (Attributes flag : flags)
            verts += flag.size;
        return verts;
    }

    public static void load(Attributes[] flags, int vertexSize) {
        //prepare vars
        int stride = vertexSize * Float.BYTES;
        int pointer = 0;
        int index = 0;

        //create attributes
        for (Attributes flag : flags) {
            glVertexAttribPointer(index++, flag.size, GL_FLOAT, flag.normalized, stride, pointer);
            pointer += flag.sizeInBytes;
        }
    }

    public static void pushVertex(FloatBuffer buffer, Vertex vertex, int textureID, Attributes[] flags) {
        for (Attributes flag : flags) {
            if (flag.consumer != null)
                flag.consumer.accept(vertex, buffer);
            else if (flag == TEXTURE_ID)
                buffer.put(textureID);
        }
    }
}
