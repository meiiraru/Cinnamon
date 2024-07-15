package mayo.gui.widgets.types;

import mayo.Client;
import mayo.gui.widgets.GUIListener;
import mayo.gui.widgets.SelectableWidget;
import mayo.model.GeometryHelper;
import mayo.render.Font;
import mayo.render.MatrixStack;
import mayo.render.batch.VertexConsumer;
import mayo.text.Style;
import mayo.text.Text;
import mayo.utils.Colors;
import mayo.utils.Resource;
import mayo.utils.TextUtils;
import mayo.utils.UIHelper;

import java.util.function.Consumer;
import java.util.function.Predicate;

import static org.lwjgl.glfw.GLFW.*;

public class TextField extends SelectableWidget {

    private static final Resource TEXTURE = new Resource("textures/gui/widgets/text_field.png");
    private static final int
            POINTER_WIDTH = 1,
            INSERT_WIDTH = 4;
    private static final Style HINT_STYLE = Style.EMPTY.italic(true).color(Colors.LIGHT_BLACK);
    private static final String PASSWORD_CHAR = "\u2022";

    private final Font font;
    private Text hintText = null;
    private String currText = "";
    private Style style = Style.EMPTY;
    private Integer borderColor;
    private int cursor = 0;
    private Consumer<String> changeListener;
    private boolean textOnly;
    private Predicate<String> filter = Filter.ANY.predicate;
    private boolean insert;
    private boolean password;

    public TextField(int x, int y, int width, int height, Font font) {
        super(x, y, width, height);
        this.font = font;
    }

    @Override
    public void renderWidget(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        if (!textOnly) renderBackground(matrices, mouseX, mouseY, delta);
        renderText(matrices, mouseX, mouseY, delta);
        if (!textOnly) renderOverlay(matrices, mouseX, mouseY, delta);
    }

    protected void renderBackground(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        UIHelper.nineQuad(
                VertexConsumer.GUI, matrices, TEXTURE,
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
                VertexConsumer.GUI, matrices, TEXTURE,
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
            String str = password ? PASSWORD_CHAR.repeat(currText.length()) : currText;

            //render text
            Text text = Text.empty().withStyle(style).append(Text.of(str));
            font.render(VertexConsumer.FONT, matrices, x, y, text);

            //offset x based on the cursor
            Text index = Text.empty().withStyle(style).append(Text.of(str.substring(0, cursor)));
            x += TextUtils.getWidth(index, font);
        }

        //render pointer
        renderPointer(matrices, x, y - 1, font.lineHeight + 2);
    }

    protected void renderPointer(MatrixStack matrices, float x, float y, float height) {
        if (isFocused() && Client.getInstance().ticks % 20 < 10) {
            matrices.push();
            matrices.translate(0, 0, UIHelper.Z_OFFSET);
            GeometryHelper.rectangle(VertexConsumer.GUI, matrices, x, y, x + (insert ? INSERT_WIDTH : POINTER_WIDTH), y + height, borderColor == null ? -1 : borderColor);
            matrices.pop();
        }
    }

    public void setHintText(String hintText) {
        setHintText(hintText == null ? null : Text.of(hintText));
    }

    public void setHintText(Text hintText) {
        this.hintText = hintText == null ? null : Text.empty().withStyle(HINT_STYLE).append(hintText);
    }

    public void setStyle(Style style) {
        this.style = style == null ? Style.EMPTY : style;
    }

    public Style getStyle() {
        return style;
    }

    public void setCursorPosition(int index) {
        this.cursor = Math.max(Math.min(index, currText.length()), 0);
    }

    public void setBorderColor(Colors color) {
        this.setBorderColor(color.rgba);
    }

    public void setBorderColor(Integer color) {
        this.borderColor = color;
    }

    public void setListener(Consumer<String> changeListener) {
        this.changeListener = changeListener;
    }

    public void setTextOnly(boolean textOnly) {
        this.textOnly = textOnly;
    }

    public void setFilter(Filter filter) {
        setFilter(filter.predicate);
    }

    public void setFilter(Predicate<String> predicate) {
        this.filter = predicate;
    }

    public void setPassword(boolean password) {
        this.password = password;
    }

    @Override
    public GUIListener keyPress(int key, int scancode, int action, int mods) {
        if (!isFocused() || action == GLFW_RELEASE)
            return super.keyPress(key, scancode, action, mods);

        boolean ctrl = (mods & GLFW_MOD_CONTROL) == 2;

        switch (key) {
            //navigation
            case GLFW_KEY_LEFT -> {
                if (ctrl) {
                    cursor = getPreviousWord(cursor);
                } else {
                    cursor = Math.max(cursor - 1, 0);
                }
                return this;
            }
            case GLFW_KEY_RIGHT -> {
                if (ctrl) {
                    cursor = getNextWord(cursor);
                } else {
                    cursor = Math.min(cursor + 1, currText.length());
                }
                return this;
            }
            case GLFW_KEY_HOME, GLFW_KEY_PAGE_UP -> {
                cursor = 0;
                return this;
            }
            case GLFW_KEY_END, GLFW_KEY_PAGE_DOWN -> {
                cursor = currText.length();
                return this;
            }
            //editing
            case GLFW_KEY_BACKSPACE -> {
                if (ctrl) {
                    int prev = getPreviousWord(cursor);
                    remove(cursor - prev);
                } else {
                    remove(1);
                }
                return this;
            }
            case GLFW_KEY_DELETE -> {
                if (ctrl) {
                    int next = getNextWord(cursor);
                    remove(cursor - next);
                } else {
                    remove(-1);
                }
                return this;
            }
            case GLFW_KEY_INSERT -> {
                insert = !insert;
                return this;
            }
            //commands
            case GLFW_KEY_V -> {
                if (ctrl) {
                    String clipboard = glfwGetClipboardString(-1);
                    if (clipboard != null) {
                        append(clipboard.replaceAll("\r\n", " "));
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
            return super.charTyped(c, mods);

        if (insert) insert(c);
        else append(String.valueOf(c));
        return this;
    }

    private int getPreviousWord(int i) {
        if (password)
            return 0;

        while (i > 0 && currText.charAt(i - 1) == ' ')
            i--;
        while (i > 0 && currText.charAt(i - 1) != ' ')
            i--;
        return i;
    }

    private int getNextWord(int i) {
        if (password)
            return currText.length();

        while (i < currText.length() && currText.charAt(i) == ' ')
            i++;
        while (i < currText.length() && currText.charAt(i) != ' ')
            i++;
        return i;
    }

    private void undo() {

    }

    private void redo() {

    }

    private void insert(char c) {
        if (cursor == currText.length()) {
            append(String.valueOf(c));
            return;
        }

        String newText = currText.substring(0, cursor) + c + currText.substring(cursor + 1);
        if (setText(newText))
            cursor++;
    }

    public void append(String s) {
        String newText = currText.substring(0, cursor) + s + currText.substring(cursor);
        if (setText(newText))
            cursor += s.length();
    }

    public void remove(int count) {
        int len = currText.length();
        if (len > 0) {
            if (count < 0) {
                if (cursor < len)
                    setText(currText.substring(0, cursor) + currText.substring(Math.min(cursor - count, len)));
            } else {
                if (cursor > 0) {
                    int amount = Math.max(cursor - count, 0);
                    if (setText(currText.substring(0, amount) + currText.substring(cursor)))
                        cursor = amount;
                }
            }
        }
    }

    private void selectClosestToMouse() {
        int x0 = getX() + 2;
        int mousePos = Client.getInstance().window.mouseX;
        Text text = Text.empty().withStyle(style).append(Text.of(password ? PASSWORD_CHAR.repeat(currText.length()) : currText));
        Text clamped = font.clampToWidth(text, mousePos - x0);
        cursor = clamped.asString().length();
    }

    @Override
    public GUIListener mousePress(int button, int action, int mods) {
        if (isActive() && isHovered() && action == GLFW_PRESS && button == GLFW_MOUSE_BUTTON_1) {
            UIHelper.focusWidget(this);
            selectClosestToMouse();
            return this;
        }

        return super.mousePress(button, action, mods);
    }

    public String getText() {
        return currText;
    }

    public boolean setText(String string) {
        if (!filter.test(string))
            return false;

        currText = string;
        if (changeListener != null)
            changeListener.accept(currText);

        return true;
    }

    public enum Filter {
        ANY(s -> true),
        INTEGER(s -> s.matches("^-?\\d*$")),
        FLOAT(s -> s.matches("^-?\\d*\\.?\\d*$")),
        AZ(s -> s.matches("^[a-zA-Z]*$")),
        HEX_COLOR(s -> s.matches("^#?[0-9a-fA-F]{0,6}$")),
        HEX_COLOR_ALPHA(s -> s.matches("^#?[0-9a-fA-F]{0,8}$"));

        private final Predicate<String> predicate;

        Filter(Predicate<String> predicate) {
            this.predicate = predicate;
        }
    }
}
