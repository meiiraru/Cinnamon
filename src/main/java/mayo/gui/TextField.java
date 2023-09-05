package mayo.gui;

import mayo.Client;
import mayo.model.Renderable;
import mayo.render.BatchRenderer;
import mayo.render.Font;
import mayo.render.MatrixStack;
import mayo.render.Shaders;
import org.lwjgl.glfw.GLFW;

public class TextField implements Widget {

    private final Font font;
    private String currText;

    public TextField(Font font) {
        this(font, "");
    }

    public TextField(Font font, String text) {
        this.font = font;
        currText = text;
    }

    @Override
    public void render(BatchRenderer renderer, MatrixStack matrices) {
        Renderable aa = font.textOf(currText, -1, true, false);
        aa.transform.setPos(Client.getInstance().scaledWidth - font.getWidth(currText), Client.getInstance().scaledHeight - font.getHeight(currText), 1);
        renderer.addElement(Shaders.FONT, matrices, aa);
    }

    @Override
    public boolean keyPress(int key, int scancode, int action, int mods) {
        if (action == GLFW.GLFW_RELEASE)
            return false;

        boolean ctrl = (mods & GLFW.GLFW_MOD_CONTROL) == 2;

        switch (key) {
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                append("\n");
                return true;
            }
            case GLFW.GLFW_KEY_BACKSPACE -> {
                remove(1);
                return true;
            }
            case GLFW.GLFW_KEY_V -> {
                if (ctrl) {
                    String clipboard = GLFW.glfwGetClipboardString(-1);
                    if (clipboard != null) {
                        append(clipboard.replaceAll("\r\n", "\n"));
                        return true;
                    }
                }
            }
            case GLFW.GLFW_KEY_Z -> {
                if (ctrl) {
                    undo();
                    return true;
                }
            }
            case GLFW.GLFW_KEY_Y -> {
                if (ctrl) {
                    redo();
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public boolean charTyped(char c, int mods) {
        append(String.valueOf(c));
        return true;
    }

    private void undo() {

    }

    private void redo() {

    }

    private void append(String s) {
        currText += s;
    }

    private void remove(int count) {
        count = Math.min(count, currText.length());
        currText = currText.substring(0, currText.length() - count);
    }
}
