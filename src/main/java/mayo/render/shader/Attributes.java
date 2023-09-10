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
            NORMAL     = 0x10;

    public static Pair<Integer, Integer> getAttributes(int flags) {
        int e = 0, verts = 0;

        if ((flags & POS)        == POS)        {e++; verts += 3;}
        if ((flags & TEXTURE_ID) == TEXTURE_ID) {e++; verts += 1;}
        if ((flags & UV)         == UV)         {e++; verts += 2;}
        if ((flags & COLOR)      == COLOR)      {e++; verts += 3;}
        if ((flags & NORMAL)     == NORMAL)     {e++; verts += 3;}

        return Pair.of(e, verts);
    }

    public static void load(int flags, int vertexSize) {
        //prepare vars
        int stride = vertexSize * Float.BYTES;
        int pointer = 0;
        int index = 0;

        //create attributes
        if ((flags & POS) == POS) {
            glVertexAttribPointer(index++, 3, GL_FLOAT, false, stride, pointer * Float.BYTES);
            pointer += 3;
        }
        if ((flags & TEXTURE_ID) == TEXTURE_ID) {
            glVertexAttribPointer(index++, 1, GL_FLOAT, false, stride, pointer * Float.BYTES);
            pointer += 1;
        }
        if ((flags & UV) == UV) {
            glVertexAttribPointer(index++, 2, GL_FLOAT, false, stride, pointer * Float.BYTES);
            pointer += 2;
        }
        if ((flags & COLOR) == COLOR) {
            glVertexAttribPointer(index++, 3, GL_FLOAT, false, stride, pointer * Float.BYTES);
            pointer += 3;
        }
        if ((flags & NORMAL) == NORMAL) {
            glVertexAttribPointer(index, 3, GL_FLOAT, false, stride, pointer * Float.BYTES);
        }
    }
}
