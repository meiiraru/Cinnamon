package mayo.gui.widgets;

import mayo.Client;
import mayo.model.Renderable;
import mayo.render.BatchRenderer;
import mayo.render.Font;
import mayo.render.MatrixStack;
import mayo.render.Shaders;
import mayo.text.Style;
import mayo.text.Text;
import mayo.utils.TextUtils;

import static org.lwjgl.glfw.GLFW.*;

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
        Text t = Text.of(currText).withStyle(
                Style.EMPTY
                        .color(-1)
                        .shadowColor(0xFF7272)
                        .outlineColor(0xAD72FF)
                        .backgroundColor(0x72ADFF)
                        .italic(true)
                        .outlined(false)
                        .shadow(true)
                        .bold(true)
                        .obfuscated(false)
                        .strikethrough(false)
                        .underlined(false)
                        .background(true)
        ).append(Text.of("\u2588TEST\u2588\nowo\nmrrow?\nvida triste >~< aaaaaaaaaa").withStyle(
                Style.EMPTY
                        .color(0XFF72AD)
                        .shadowColor(0x884157)
                        .backgroundColor(0xFFFF72)
                        .italic(true)
        ).append(Text.of(":3").withStyle(Style.EMPTY.color(0x72FFAD).obfuscated(true))));

        Renderable aa = font.bake(t, TextUtils.Alignment.RIGHT);
        aa.transform.setPos(10, Client.getInstance().scaledHeight - font.height(t) - 10, 0);
        renderer.addElement(Shaders.FONT, matrices, aa);
    }

    @Override
    public boolean keyPress(int key, int scancode, int action, int mods) {
        if (action == GLFW_RELEASE)
            return false;

        boolean ctrl = (mods & GLFW_MOD_CONTROL) == 2;

        switch (key) {
            case GLFW_KEY_ENTER, GLFW_KEY_KP_ENTER -> {
                append("\n");
                return true;
            }
            case GLFW_KEY_BACKSPACE -> {
                remove(1);
                return true;
            }
            case GLFW_KEY_V -> {
                if (ctrl) {
                    String clipboard = glfwGetClipboardString(-1);
                    if (clipboard != null) {
                        append(clipboard.replaceAll("\r\n", "\n"));
                        return true;
                    }
                }
            }
            case GLFW_KEY_Z -> {
                if (ctrl) {
                    undo();
                    return true;
                }
            }
            case GLFW_KEY_Y -> {
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
