package cinnamon.render.texture;

import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.GL_TEXTURE_2D_ARRAY;

public class TextureArray {

    public static int bind(int id, int index) {
        glActiveTexture(GL_TEXTURE0 + index);
        glBindTexture(GL_TEXTURE_2D_ARRAY, id);
        return index;
    }

    public static void unbindTex(int index) {
        bind(0, index);
    }
}
