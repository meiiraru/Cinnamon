package cinnamon.render.shader;

import static org.lwjgl.opengl.GL11.*;

public enum DataType {
    FLOAT         (GL_FLOAT,          4, false),
    INT           (GL_INT,            4),
    UNSIGNED_INT  (GL_UNSIGNED_INT,   4),
    SHORT         (GL_SHORT,          2),
    UNSIGNED_SHORT(GL_UNSIGNED_SHORT, 2),
    BYTE          (GL_BYTE,           1),
    UNSIGNED_BYTE (GL_UNSIGNED_BYTE,  1);

    public final int glType;
    public final int byteSize;
    public final boolean isInteger;

    DataType(int glType, int byteSize) {
        this(glType, byteSize, true);
    }

    DataType(int glType, int byteSize, boolean isInteger) {
        this.glType    = glType;
        this.byteSize  = byteSize;
        this.isInteger = isInteger;
    }
}
