package cinnamon.render.shader;

import cinnamon.model.Vertex;
import java.nio.ByteBuffer;

import static cinnamon.render.shader.DataType.*;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glVertexAttribIPointer;

public class VertexAttribute {

    private final int componentCount;
    private final DataType type;
    private final boolean normalized;
    private final AttributeWriter writer;

    //default attributes

    public static final VertexAttribute
            POS = new VertexAttribute(3, FLOAT, false, (v, _, b) -> {
                b.putFloat(v.getPos().x);
                b.putFloat(v.getPos().y);
                b.putFloat(v.getPos().z);
            }),
            POS_XY = new VertexAttribute(2, FLOAT, false, (v, _, b) -> {
                b.putFloat(v.getPos().x);
                b.putFloat(v.getPos().y);
            }),
            TEXTURE_ID = new VertexAttribute(1, INT, false, (_, texID, b) ->
                b.putInt(texID)),
            UV = new VertexAttribute(2, FLOAT, false, (v, _, b) -> {
                b.putFloat(v.getUV().x);
                b.putFloat(v.getUV().y);
            }),
            UV_FLIP = new VertexAttribute(2, FLOAT, false, (v, _, b) -> {
                b.putFloat(v.getUV().x);
                b.putFloat(1f - v.getUV().y);
            }),
            COLOR = new VertexAttribute(3, UNSIGNED_BYTE, true, (v, _, b) -> {
                b.put((byte) (v.getColor().x * 255f));
                b.put((byte) (v.getColor().y * 255f));
                b.put((byte) (v.getColor().z * 255f));
            }),
            COLOR_RGBA = new VertexAttribute(4, UNSIGNED_BYTE, true, (v, _, b) -> {
                b.put((byte) (v.getColor().x * 255f));
                b.put((byte) (v.getColor().y * 255f));
                b.put((byte) (v.getColor().z * 255f));
                b.put((byte) (v.getColor().w * 255f));
            }),
            NORMAL = new VertexAttribute(3, FLOAT, false, (v, _, b) -> {
                b.putFloat(v.getNormal().x);
                b.putFloat(v.getNormal().y);
                b.putFloat(v.getNormal().z);
            }),
            TANGENTS = new VertexAttribute(3, FLOAT, false, (v, _, b) -> {
                b.putFloat(v.getTangent().x);
                b.putFloat(v.getTangent().y);
                b.putFloat(v.getTangent().z);
            });

    public VertexAttribute(int componentCount, DataType type, boolean normalized, AttributeWriter writer) {
        this.componentCount = componentCount;
        this.type = type;
        this.normalized = normalized;
        this.writer = writer;
    }

    public int getSizeInBytes() {
        return componentCount * type.byteSize;
    }

    public static int getStrideInBytes(VertexAttribute... attributes) {
        int stride = 0;
        for (VertexAttribute attr : attributes)
            stride += attr.getSizeInBytes();
        return stride;
    }

    public static void load(VertexAttribute... attributes) {
        int stride = getStrideInBytes(attributes);
        int pointer = 0;
        int index = 0;

        for (VertexAttribute attr : attributes) {
            if (attr.type.isInteger && !attr.normalized) {
                glVertexAttribIPointer(index++, attr.componentCount, attr.type.glType, stride, pointer);
            } else {
                glVertexAttribPointer(index++, attr.componentCount, attr.type.glType, attr.normalized, stride, pointer);
            }
            pointer += attr.getSizeInBytes();
        }
    }

    public static void pushVertex(ByteBuffer buffer, Vertex vertex, int textureID, VertexAttribute... attributes) {
        for (VertexAttribute attr : attributes) {
            if (attr.writer != null)
                attr.writer.write(vertex, textureID, buffer);
        }
    }

    public interface AttributeWriter {
        void write(Vertex vertex, int textureID, ByteBuffer buffer);
    }
}
