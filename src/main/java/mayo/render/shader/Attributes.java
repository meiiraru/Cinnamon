package mayo.render.shader;

import mayo.utils.Pair;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;

public class Attributes {

    //vertex
    //pos     tex id    uv      color    normal
    //vec3    int       vec2    vec3     vec3
    public static final int
            POS        = 0x1,
            TEXTURE_ID = 0x2,
            UV         = 0x4,
            COLOR      = 0x8,
            COLOR_RGBA = 0x10,
            NORMAL     = 0x20,
            INDEX      = 0x40;

    public static Pair<Integer, Integer> getAttributes(int flags) {
        int e = 0, verts = 0;

        if ((flags & POS)        == POS)        {e++; verts += 3;}
        if ((flags & TEXTURE_ID) == TEXTURE_ID) {e++; verts += 1;}
        if ((flags & UV)         == UV)         {e++; verts += 2;}
        if ((flags & COLOR)      == COLOR)      {e++; verts += 3;}
        if ((flags & COLOR_RGBA) == COLOR_RGBA) {e++; verts += 4;}
        if ((flags & NORMAL)     == NORMAL)     {e++; verts += 3;}
        if ((flags & INDEX)      == INDEX)      {e++; verts += 1;}

        return Pair.of(e, verts);
    }

    public static void load(int flags, int vertexSize) {
        //prepare vars
        int stride = vertexSize * Float.BYTES;
        int pointer = 0;
        int index = 0;

        //create attributes
        if ((flags & POS) == POS) {
            glVertexAttribPointer(index++, 3, GL_FLOAT, false, stride, 0);
            pointer += 3 * Float.BYTES;
        }
        if ((flags & TEXTURE_ID) == TEXTURE_ID) {
            glVertexAttribPointer(index++, 1, GL_FLOAT, true, stride, pointer);
            pointer += Float.BYTES;
        }
        if ((flags & UV) == UV) {
            glVertexAttribPointer(index++, 2, GL_FLOAT, false, stride, pointer);
            pointer += 2 * Float.BYTES;
        }
        if ((flags & COLOR) == COLOR) {
            glVertexAttribPointer(index++, 3, GL_FLOAT, false, stride, pointer);
            pointer += 3 * Float.BYTES;
        }
        if ((flags & COLOR_RGBA) == COLOR_RGBA) {
            glVertexAttribPointer(index++, 4, GL_FLOAT, false, stride, pointer);
            pointer += 4 * Float.BYTES;
        }
        if ((flags & NORMAL) == NORMAL) {
            glVertexAttribPointer(index, 3, GL_FLOAT, false, stride, pointer);
            pointer += 3 * Float.BYTES;
        }
        if ((flags & INDEX) == INDEX) {
            glVertexAttribPointer(index, 1, GL_FLOAT, true, stride, pointer);
        }
    }
}
