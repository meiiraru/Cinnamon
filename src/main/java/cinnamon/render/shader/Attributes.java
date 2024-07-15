package cinnamon.render.shader;

import cinnamon.model.Vertex;
import cinnamon.utils.Pair;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;

public class Attributes {

    //vertex
    //pos     posXY    tex id    uv      color    colorRGBA    normal    index    tangents
    //vec3    vec2     int       vec2    vec3     vec4         vec3      int      vec3
    public static final int
            POS        = 0x1,
            POS_XY     = 0x2,
            TEXTURE_ID = 0x4,
            UV         = 0x8,
            COLOR      = 0x10,
            COLOR_RGBA = 0x20,
            NORMAL     = 0x40,
            INDEX      = 0x80,
            TANGENTS   = 0x100;

    public static Pair<Integer, Integer> getAttributes(int flags) {
        int e = 0, verts = 0;

        if ((flags & POS)        != 0) {e++; verts += 3;}
        if ((flags & POS_XY)     != 0) {e++; verts += 2;}
        if ((flags & TEXTURE_ID) != 0) {e++; verts += 1;}
        if ((flags & UV)         != 0) {e++; verts += 2;}
        if ((flags & COLOR)      != 0) {e++; verts += 3;}
        if ((flags & COLOR_RGBA) != 0) {e++; verts += 4;}
        if ((flags & NORMAL)     != 0) {e++; verts += 3;}
        if ((flags & INDEX)      != 0) {e++; verts += 1;}
        if ((flags & TANGENTS)   != 0) {e++; verts += 3;}

        return Pair.of(e, verts);
    }

    public static void load(int flags, int vertexSize) {
        //prepare vars
        int stride = vertexSize * Float.BYTES;
        int pointer = 0;
        int index = 0;

        //create attributes
        if ((flags & POS) != 0) {
            glVertexAttribPointer(index++, 3, GL_FLOAT, false, stride, pointer);
            pointer += 3 * Float.BYTES;
        }
        if ((flags & POS_XY) != 0) {
            glVertexAttribPointer(index++, 2, GL_FLOAT, false, stride, pointer);
            pointer += 2 * Float.BYTES;
        }
        if ((flags & TEXTURE_ID) != 0) {
            glVertexAttribPointer(index++, 1, GL_FLOAT, true, stride, pointer);
            pointer += Float.BYTES;
        }
        if ((flags & UV) != 0) {
            glVertexAttribPointer(index++, 2, GL_FLOAT, false, stride, pointer);
            pointer += 2 * Float.BYTES;
        }
        if ((flags & COLOR) != 0) {
            glVertexAttribPointer(index++, 3, GL_FLOAT, false, stride, pointer);
            pointer += 3 * Float.BYTES;
        }
        if ((flags & COLOR_RGBA) != 0) {
            glVertexAttribPointer(index++, 4, GL_FLOAT, false, stride, pointer);
            pointer += 4 * Float.BYTES;
        }
        if ((flags & NORMAL) != 0) {
            glVertexAttribPointer(index++, 3, GL_FLOAT, false, stride, pointer);
            pointer += 3 * Float.BYTES;
        }
        if ((flags & INDEX) != 0) {
            glVertexAttribPointer(index++, 1, GL_FLOAT, true, stride, pointer);
            pointer += Float.BYTES;
        }
        if ((flags & TANGENTS) != 0) {
            glVertexAttribPointer(index++, 3, GL_FLOAT, false, stride, pointer);
            pointer += 3 * Float.BYTES;
        }
    }

    public static void pushVertex(FloatBuffer buffer, Vertex vertex, int textureID, int flags) {
        //push pos
        if ((flags & POS) != 0) {
            Vector3f pos = vertex.getPosition();
            buffer.put(pos.x);
            buffer.put(pos.y);
            buffer.put(pos.z);
        }

        //push posXY
        if ((flags & POS_XY) != 0) {
            Vector3f pos = vertex.getPosition();
            buffer.put(pos.x);
            buffer.put(pos.y);
        }

        //push texture id
        if ((flags & TEXTURE_ID) != 0) {
            buffer.put(textureID);
        }

        //push uv
        if ((flags & UV) != 0) {
            Vector2f uv = vertex.getUV();
            buffer.put(uv.x);
            buffer.put(uv.y);
        }

        //push color RGB
        if ((flags & COLOR) != 0) {
            Vector4f color = vertex.getColor();
            buffer.put(color.x);
            buffer.put(color.y);
            buffer.put(color.z);
        }

        //push color RGBA
        if ((flags & COLOR_RGBA) != 0) {
            Vector4f color = vertex.getColor();
            buffer.put(color.x);
            buffer.put(color.y);
            buffer.put(color.z);
            buffer.put(color.w);
        }

        //push normal
        if ((flags & NORMAL) != 0) {
            Vector3f normal = vertex.getNormal();
            buffer.put(normal.x);
            buffer.put(normal.y);
            buffer.put(normal.z);
        }

        //push index
        if ((flags & INDEX) != 0) {
            buffer.put(vertex.getIndex());
        }
    }
}
