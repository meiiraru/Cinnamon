package cinnamon.render;

import static org.lwjgl.opengl.GL11.*;

public class StencilRenderer {

    public static void prepareStencil(boolean allowDrawing, boolean clear) {
        if (!allowDrawing) {
            glColorMask(false, false, false, false);
            glDepthMask(false);
        }
        glStencilMask(0xFF);

        glEnable(GL_STENCIL_TEST);
        glDisable(GL_DEPTH_TEST);
        if (clear) glClear(GL_STENCIL_BUFFER_BIT);
        glStencilOp(GL_KEEP, GL_KEEP, GL_REPLACE);
        glStencilFunc(GL_ALWAYS, 1, 0xFF);
    }

    public static void lockStencil(boolean inverted) {
        glColorMask(true, true, true, true);
        glDepthMask(true);
        glStencilMask(0x00);

        glStencilFunc(inverted ? GL_NOTEQUAL : GL_EQUAL, 1, 0xFF);

        glEnable(GL_DEPTH_TEST);
    }

    public static void disableStencil() {
        glDisable(GL_STENCIL_TEST);
    }
}
