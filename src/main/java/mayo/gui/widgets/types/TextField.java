package mayo.gui.widgets.types;

import mayo.gui.widgets.GUIListener;
import mayo.gui.widgets.Widget;
import mayo.render.Font;
import mayo.render.MatrixStack;
import mayo.render.batch.VertexConsumer;
import mayo.text.Style;
import mayo.text.Text;
import mayo.utils.TextUtils;

import static org.lwjgl.glfw.GLFW.*;

public class TextField extends Widget implements GUIListener {

    private final Font font;
    private String currText;

    public TextField(Font font) {
        this(font, "");
    }

    public TextField(Font font, String text) {
        super(50, 50, 50, 50);
        this.font = font;
        currText = text;
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        Text t = Text.empty().append(Text.of("Lorem ipsum").withStyle(
                Style.EMPTY
                        .backgroundColor(0xFF72ADFF)
                        .shadowColor(0xFFFF7272)
                        .background(true)
                        .shadow(true)
                        .bold(true)
        ));

        t.append(Text.of(" dolor sit amet.\nSit quae dignissimos non voluptates sunt").withStyle(
                Style.EMPTY
                        .color(0xFF72FFAD)
        ).append(Text.of("\nut temporibus commodi eum galisum").withStyle(
                Style.EMPTY
                        .backgroundColor(0xFFFF72AD)
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
                        .outlineColor(0xFF72ADFF)
                        .outlined(true)
                        .italic(true)
        ).append(Text.of("\nAb accusamus ad alias aperiam\n[...]").withStyle(
                Style.EMPTY
                        .backgroundColor(0xFF72FF72)
                        .color(0xFF202020)
                        .bold(true)
                        .background(true)
                        .italic(false)
        )));

        t.append(Text.of("\n\niii OBFUSCATED iii").withStyle(
                Style.EMPTY
                        .backgroundColor(0xFFAD72FF)
                        .background(true)
                        .obfuscated(true)
        ));

        t.append(Text.of("\n\n\u306F\u3058\u3081\u307E\u3057\u3066\u3000\u308F\u305F\u3057\u306F\u3000\u3081\u3044\u3067\u3059\u3000\u3088\u308D\u3057\u304F\u3000\u304A\u306D\u304C\u3044\u3000\u3057\u307E\u3059~~").withStyle(
                Style.EMPTY
                        .strikethrough(false)
        ));

        t.append("\n\n").append(currText);

        font.render(VertexConsumer.FONT_FLAT, matrices, getX(), getY(), t, TextUtils.Alignment.CENTER);
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
