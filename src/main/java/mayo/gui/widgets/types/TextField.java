package mayo.gui.widgets.types;

import mayo.Client;
import mayo.gui.widgets.GUIListener;
import mayo.gui.widgets.SelectableWidget;
import mayo.render.Font;
import mayo.render.MatrixStack;
import mayo.render.Texture;
import mayo.render.batch.VertexConsumer;
import mayo.text.Style;
import mayo.text.Text;
import mayo.utils.Colors;
import mayo.utils.Resource;
import mayo.utils.TextUtils;
import mayo.utils.UIHelper;

import java.util.function.Consumer;

import static org.lwjgl.glfw.GLFW.*;

public class TextField extends SelectableWidget {

    private static final Texture TEXTURE = Texture.of(new Resource("textures/gui/widgets/text_field.png"));
    private static final String POINTER = "|";
    private static final Style HINT_STYLE = Style.EMPTY.italic(true).color(Colors.LIGHT_BLACK);

    private final Font font;
    private Text hintText = null;
    private String currText = "";
    private Style style = Style.EMPTY;
    private Style colorStyle = Style.EMPTY;
    private Integer borderColor;
    private int selectedIndex = 0;
    private Consumer<String> changeListener;

    public TextField(Font font, int x, int y, int width, int height) {
        super(x, y, width, height);
        this.font = font;
    }

    @Override
    public void renderWidget(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices, mouseX, mouseY, delta);
        renderText(matrices, mouseX, mouseY, delta);
        renderOverlay(matrices, mouseX, mouseY, delta);
    }

    protected void renderBackground(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        UIHelper.nineQuad(
                VertexConsumer.GUI, matrices, TEXTURE.getID(),
                getX(), getY(),
                getWidth(), getHeight(),
                getState() * 16f, 0f,
                16, 16,
                64, 16
        );
    }

    protected void renderOverlay(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        if (borderColor == null || this.isHoveredOrFocused())
            return;

        UIHelper.nineQuad(
                VertexConsumer.GUI, matrices, TEXTURE.getID(),
                getX(), getY(),
                getWidth(), getHeight(),
                48f, 0f,
                16, 16,
                64, 16,
                borderColor
        );
    }

    protected void renderText(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        float x = getX() + 2;
        float y = getCenterY() - font.lineHeight * 0.5f;

        //hint text
        if (currText.isEmpty()) {
            if (hintText != null)
                font.render(VertexConsumer.FONT, matrices, x, y, hintText);
        } else {
            //render text
            Text text = Text.empty().withStyle(colorStyle).append(Text.of(currText));
            font.render(VertexConsumer.FONT, matrices, x, y, text);

            //offset x
            x += TextUtils.getWidth(text, font);
        }

        //render pointer
        renderPointer(matrices, x, y);
    }

    protected void renderPointer(MatrixStack matrices, float x, float y) {
        if (isFocused() && Client.getInstance().ticks % 20 < 10)
            font.render(VertexConsumer.FONT, matrices, x, y, Text.empty().withStyle(colorStyle).append(Text.of(POINTER).withStyle(style)));
    }

    protected int getState() {
        if (!isActive())
            return 0;
        else if (isHoveredOrFocused())
            return 2;
        else
            return 1;
    }

    public void setHintText(String hintText) {
        this.hintText = hintText == null ? null : Text.of(hintText).withStyle(HINT_STYLE);
    }

    public void setStyle(Style style) {
        this.style = style == null ? Style.EMPTY : style;
    }

    public void setSelectedIndex(int selectedIndex) {
        this.selectedIndex = Math.max(Math.min(selectedIndex, currText.length()), 0);
    }

    public void setColor(int color) {
        this.colorStyle = Style.EMPTY.color(color);
    }

    public void setBorderColor(Integer color) {
        this.borderColor = color;
    }

    public void setListener(Consumer<String> changeListener) {
        this.changeListener = changeListener;
    }

    @Override
    public GUIListener keyPress(int key, int scancode, int action, int mods) {
        if (!isFocused() || action == GLFW_RELEASE)
            return super.keyPress(key, scancode, action, mods);

        boolean ctrl = (mods & GLFW_MOD_CONTROL) == 2;

        switch (key) {
            case GLFW_KEY_ENTER, GLFW_KEY_KP_ENTER -> {
                append("\n");
                return this;
            }
            case GLFW_KEY_BACKSPACE -> {
                remove(1);
                return this;
            }
            case GLFW_KEY_V -> {
                if (ctrl) {
                    String clipboard = glfwGetClipboardString(-1);
                    if (clipboard != null) {
                        append(clipboard.replaceAll("\r\n", "\n"));
                        return this;
                    }
                }
            }
            case GLFW_KEY_Z -> {
                if (ctrl) {
                    undo();
                    return this;
                }
            }
            case GLFW_KEY_Y -> {
                if (ctrl) {
                    redo();
                    return this;
                }
            }
        }

        return super.keyPress(key, scancode, action, mods);
    }

    @Override
    public GUIListener charTyped(char c, int mods) {
        if (!isFocused())
            return super.charTyped(c, mods);;

        append(String.valueOf(c));
        return this;
    }

    private void undo() {

    }

    private void redo() {

    }

    private void append(String s) {
        currText += s;
        if (changeListener != null)
            changeListener.accept(currText);
    }

    private void remove(int count) {
        int len = currText.length();
        if (len > 0) {
            currText = currText.substring(0, len - Math.min(count, len));
            if (changeListener != null)
                changeListener.accept(currText);
        }
    }

    @Override
    public GUIListener mousePress(int button, int action, int mods) {
        if (isActive() && isHovered() && action == GLFW_PRESS && button == GLFW_MOUSE_BUTTON_1) {
            UIHelper.focusWidget(this);
            return this;
        }

        return super.mousePress(button, action, mods);
    }

    public String getString() {
        return currText;
    }

    public void setString(String string) {
        this.currText = string;
    }
}
