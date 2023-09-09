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
        Text t = Text.empty().append(Text.of("Lorem ipsum").withStyle(
                Style.EMPTY
                        .backgroundColor(0x72ADFF)
                        .shadowColor(0xFF7272)
                        .background(true)
                        .shadow(true)
                        .bold(true)
        ));

        t.append(Text.of(" dolor sit amet.\nSit quae dignissimos non voluptates sunt").withStyle(
                Style.EMPTY
                        .color(0x72FFAD)
        ).append(Text.of("\nut temporibus commodi eum galisum").withStyle(
                Style.EMPTY
                        .backgroundColor(0xFF72AD)
                        .background(true)
                        .outlined(true)
        )));

        t.append(Text.of(" alias.").withStyle(
                Style.EMPTY
                        .bold(true)
                        .italic(true)
                        .underlined(true)
        ));

        t.append("\n\n");

        t.append(Text.of("Lorem ipsum dolor sit amet,\nconsectetur adipisicing elit.").withStyle(
                Style.EMPTY
                        .outlineColor(0x72ADFF)
                        .outlined(true)
                        .italic(true)
        ).append(Text.of("\nAb accusamus ad alias aperiam\n[...]").withStyle(
                Style.EMPTY
                        .backgroundColor(0x72FF72)
                        .color(0x202020)
                        .bold(true)
                        .background(true)
                        .italic(false)
        )));

        t.append(Text.of("\n\niii OBFUSCATED iii").withStyle(
                Style.EMPTY
                        .backgroundColor(0xAD72FF)
                        .background(true)
                        .obfuscated(true)
        ));

        t.append("\n\n").append(currText);

        Renderable aa = font.bake(t, TextUtils.Alignment.CENTER);
        aa.transform.setPos((int) (Client.getInstance().scaledWidth / 2f), (int) (Client.getInstance().scaledHeight - font.height(t) - 10f), 0);
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
