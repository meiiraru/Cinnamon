package cinnamon.gui.widgets.types;

import cinnamon.Client;
import cinnamon.gui.widgets.GUIListener;
import cinnamon.gui.widgets.SelectableWidget;
import cinnamon.model.GeometryHelper;
import cinnamon.render.Font;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.text.Style;
import cinnamon.text.Text;
import cinnamon.utils.Colors;
import cinnamon.utils.Resource;
import cinnamon.utils.TextUtils;
import cinnamon.utils.UIHelper;

import java.util.function.Consumer;
import java.util.function.Predicate;

import static org.lwjgl.glfw.GLFW.*;

public class TextField extends SelectableWidget {

    private static final Resource TEXTURE = new Resource("textures/gui/widgets/text_field.png");
    private static final int
            CURSOR_WIDTH = 1,
            INSERT_WIDTH = 4,
            BLINK_SPEED = 20,
            HISTORY_SIZE = 20;
    private static final Style HINT_STYLE = Style.EMPTY.italic(true).color(Colors.LIGHT_BLACK);
    private static final String PASSWORD_CHAR = "\u2022";
    private static final Predicate<Character> WORD_CHARACTERS = c -> Character.isAlphabetic(c) || Character.isDigit(c) || c == '_';

    private final Font font;

    //text
    private String currText = "";
    private int cursor;
    private boolean insert;
    private Consumer<String> changeListener;
    private Predicate<Character> filter = Filter.ANY.predicate;
    private int charLimit = Integer.MAX_VALUE;
    private Consumer<TextField> enterListener;

    //history
    private final String[] history = new String[HISTORY_SIZE];
    private int historyIndex;
    private Action lastAction = null;

    //style
    private Text hintText = null;
    private Style style = Style.EMPTY;
    private Integer borderColor;
    private String formatting;
    private String formattedText = "";
    private boolean textOnly;
    private boolean password;

    public TextField(int x, int y, int width, int height, Font font) {
        super(x, y, width, height);
        this.font = font;
    }


    // -- rendering -- //


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
        if (borderColor == null || this.isHovered())
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
            String str = getFormattedText();

            //render text
            Text text = Text.of(str).withStyle(style);
            font.render(VertexConsumer.FONT, matrices, x, y, text);

            //offset x based on the cursor
            int extra = getFormattingSkippedCharCount(cursor);
            Text index = Text.of(str.substring(0, cursor + extra)).withStyle(style);
            x += TextUtils.getWidth(index, font);
        }

        //render cursor
        renderCursor(matrices, x, y - 1, font.lineHeight + 2);
    }

    protected void renderCursor(MatrixStack matrices, float x, float y, float height) {
        if (isFocused() && Client.getInstance().ticks % BLINK_SPEED < BLINK_SPEED / 2) {
            matrices.push();
            //translate matrices so we can render on top of text
            matrices.translate(0, 0, UIHelper.Z_OFFSET);
            GeometryHelper.rectangle(VertexConsumer.GUI, matrices, x, y, x + (insert ? INSERT_WIDTH : CURSOR_WIDTH), y + height, borderColor == null ? -1 : borderColor);
            matrices.pop();
        }
    }


    // -- getters and setters -- //


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

    public void setFilter(Predicate<Character> predicate) {
        this.filter = predicate == null ? Filter.ANY.predicate : predicate;
    }

    public void setPassword(boolean password) {
        this.password = password;
    }

    public void setFormatting(String formatting) {
        this.formatting = formatting;
    }

    public String getText() {
        return currText;
    }

    public String getFormattedText() {
        return formattedText;
    }

    public void setCharLimit(int charLimit) {
        this.charLimit = charLimit;
        //update text to fit the new limit
        setText(currText);
    }

    public void setEnterListener(Consumer<TextField> enterListener) {
        this.enterListener = enterListener;
    }


    // -- formatting -- //


    private String applyFormatting(String s) {
        boolean escaped = false;
        int length = s.length();
        int chars = 0;
        StringBuilder build = new StringBuilder();

        for (int i = 0; i < formatting.length() && chars < length; i++) {
            char c = formatting.charAt(i);
            if (c == '\\') {
                escaped = !escaped;
                continue;
            }

            if (escaped) {
                build.append(c);
                escaped = false;
                continue;
            }

            if (c == '#') {
                build.append(s.charAt(chars));
                chars++;
                continue;
            }

            build.append(c);
        }

        if (chars < length)
            build.append(s.substring(chars));

        return build.toString();
    }

    private int getFormattingSkippedCharCount(int cursor) {
        //no formatting, no skipped chars
        if (formatting == null)
            return 0;

        int count = 0;
        boolean escaped = false;

        //go through the formatting and count the skipped chars
        for (int i = 0, j = 0; i < formatting.length() && j < cursor; i++) {
            char c = formatting.charAt(i);
            if (c == '\\') {
                escaped = !escaped;
                continue;
            }

            if (escaped) {
                escaped = false;
                count++;
                continue;
            }

            if (c == '#') {
                j++;
                continue;
            }

            count++;
        }

        return count;
    }

    private int getFormattingExtraCharCount(int cursor) {
        //no formatting, no extra chars
        if (formatting == null)
            return 0;

        int count = 0;
        boolean escaped = false;

        //go through the formatting and count the extra chars
        for (int i = 0; i < formatting.length() && i < cursor; i++) {
            char c = formatting.charAt(i);
            if (c == '\\') {
                escaped = !escaped;
                continue;
            }
            if (escaped || c != '#') {
                escaped = false;
                count++;
            }
        }

        return count;
    }


    // -- events -- //


    @Override
    public GUIListener keyPress(int key, int scancode, int action, int mods) {
        if (!isFocused() || action == GLFW_RELEASE)
            return super.keyPress(key, scancode, action, mods);

        boolean ctrl = (mods & GLFW_MOD_CONTROL) != 0;

        switch (key) {
            //navigation
            case GLFW_KEY_LEFT -> {
                if (ctrl) { //move word
                    cursor = getPreviousWord(cursor);
                } else { //move char
                    cursor = Math.max(cursor - 1, 0);
                }
                return this;
            }
            case GLFW_KEY_RIGHT -> {
                if (ctrl) { //move word
                    cursor = getNextWord(cursor);
                } else { //move char
                    cursor = Math.min(cursor + 1, currText.length());
                }
                return this;
            }
            case GLFW_KEY_HOME, GLFW_KEY_PAGE_UP -> {
                //no lines, so page up is the same as home
                cursor = 0;
                return this;
            }
            case GLFW_KEY_END, GLFW_KEY_PAGE_DOWN -> {
                //same for page down
                cursor = currText.length();
                return this;
            }
            //editing
            case GLFW_KEY_BACKSPACE -> {
                if (ctrl) { //remove word
                    int prev = getPreviousWord(cursor);
                    remove(cursor - prev);
                } else { //remove char
                    remove(1);
                }
                return this;
            }
            case GLFW_KEY_DELETE -> {
                if (ctrl) { //remove word
                    int next = getNextWord(cursor);
                    remove(cursor - next);
                } else { //remove char
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
                        appendToHistory(Action.PASTE);
                        //no new lines allowed
                        append(clipboard.replaceAll("(\r\n)|\n", " "));
                        return this;
                    }
                }
            }
            case GLFW_KEY_Z -> {
                if (ctrl) {
                    if ((mods & GLFW_MOD_SHIFT) != 0) redo();
                    else undo();
                    return this;
                }
            }
            case GLFW_KEY_Y -> {
                if (ctrl) {
                    redo();
                    return this;
                }
            }
            //enter
            case GLFW_KEY_ENTER, GLFW_KEY_KP_ENTER -> {
                if (enterListener != null) {
                    enterListener.accept(this);
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

        if (insert) {
            insert(c);
        } else {
            appendToHistory(Action.WRITE);
            append(String.valueOf(c));
        }

        return this;
    }

    private int getPreviousWord(int i) {
        if (password) //for security, passwords do not have words
            return 0;

        //skip spaces
        while (i > 0 && Character.isWhitespace(currText.charAt(i - 1)))
            i--;

        int oldI = i;

        //skip words
        while (i > 0 && WORD_CHARACTERS.test(currText.charAt(i - 1)))
            i--;

        //if we did not move, try to skip anything else
        if (i == oldI) {
            char c;
            while (i > 0 && !Character.isWhitespace(c = currText.charAt(i - 1)) && !WORD_CHARACTERS.test(c))
                i--;
        }

        return i;
    }

    private int getNextWord(int i) {
        int len = currText.length();
        if (password) //for security, passwords do not have words
            return len;

        //skip spaces
        while (i < len && Character.isWhitespace(currText.charAt(i)))
            i++;

        int oldI = i;

        //skip words
        while (i < len && WORD_CHARACTERS.test(currText.charAt(i)))
            i++;

        //if we did not move, try to skip anything else
        if (i == oldI) {
            char c;
            while (i < len && !Character.isWhitespace(c = currText.charAt(i)) && !WORD_CHARACTERS.test(c))
                i++;
        }

        return i;
    }

    private void undo() {
        //cannot undo from here
        if (historyIndex <= 0)
            return;

        //save the undo action, however it may increase the index, so we need to save it beforehand
        int i = historyIndex - 1;
        appendToHistory(Action.UNDO);
        historyIndex = i;
        lastAction = null;

        //unwrap the text and cursor position
        String text = history[historyIndex];
        String[] split = text.split("\\|", 2);

        //set the cursor and text
        cursor = Integer.parseInt(split[0]);
        setText(split[1]);
    }

    private void redo() {
        //no redo available
        if (historyIndex >= history.length - 1 || history[historyIndex + 1] == null)
            return;

        //increase the history index
        historyIndex++;
        lastAction = null;

        //unwrap the text and cursor position
        String text = history[historyIndex];
        String[] split = text.split("\\|", 2);

        //set the cursor and text
        cursor = Integer.parseInt(split[0]);
        setText(split[1]);
    }

    private void appendToHistory(Action action) {
        //if it is the same action, we do not want to save it
        if (lastAction == action)
            return;

        //store the current action and add the cursor position to the text, then save the text
        lastAction = action;
        String text = cursor + "|" + currText;
        history[historyIndex] = text;

        //if we are on the limit of the history, we need to shift the array
        if (historyIndex == history.length - 1) {
            //except if it is an undo action, we do not want to shift it and then go back to itself
            if (action != Action.UNDO)
                System.arraycopy(history, 1, history, 0, history.length - 1);
        } else {
            //otherwise just increase the history index
            historyIndex++;
        }
    }

    private void insert(char c) {
        appendToHistory(Action.INSERT);

        //if the cursor is at the end, we can just append the char
        if (cursor == currText.length()) {
            append(String.valueOf(c));
            return;
        }

        //otherwise we test the char, then substring it inside the text
        if (filter.test(c)) {
            String newText = currText.substring(0, cursor) + c + currText.substring(cursor + 1);
            cursor++;
            setText(newText);
        }
    }

    private void append(String s) {
        StringBuilder build = new StringBuilder();

        //test each char and append it to the build
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (filter.test(c))
                build.append(c);
        }

        if (!build.isEmpty()) {
            //append the build to the text, caring for the cursor position
            String newText = currText.substring(0, cursor) + build + currText.substring(cursor);
            cursor = Math.min(cursor + s.length(), charLimit);
            setText(newText);
        }
    }

    private void remove(int count) {
        int len = currText.length();
        if (len == 0) //nothing to remove
            return;

        appendToHistory(Action.DELETE);

        //negative count means delete forward
        if (count < 0) {
            if (cursor < len)
                setText(currText.substring(0, cursor) + currText.substring(Math.min(cursor - count, len)));
        } else {
            if (cursor > 0) {
                //get the substring position
                int i = Math.max(cursor - count, 0);
                //substring out the text then add what is in the right of the cursor
                String newText = currText.substring(0, i) + currText.substring(cursor);
                //update cursor position and set the text
                cursor = i;
                setText(newText);
            }
        }
    }

    private void selectClosestToMouse() {
        //rendering text offset
        int x0 = getX() + 2;
        //grab mouse pos
        int mousePos = Client.getInstance().window.mouseX;
        //get the text
        Text text = Text.of(getFormattedText()).withStyle(style);
        //convert the mouse pos to the text space and get the length at the position
        Text clamped = font.clampToWidth(text, mousePos - x0);
        //grab the length of the text
        int length = clamped.asString().length();
        //since the length uses the formatting, we need to subtract the extra chars
        cursor = length - getFormattingExtraCharCount(length);
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

    public void setText(String string) {
        //trim the text to the char limit
        if (string.length() > charLimit)
            string = string.substring(0, charLimit);

        //do not update if the text is the same
        if (currText.equals(string))
            return;

        //update cursor inside the new boundaries then set the text
        cursor = Math.min(cursor, string.length());
        currText = string;

        //update the formatted text
        if (password)
            //password uses only the same char
            formattedText = PASSWORD_CHAR.repeat(currText.length());
        else if (formatting != null)
            //formatting have a special parser
            formattedText = applyFormatting(currText);
        else
            //otherwise just use the text
            formattedText = currText;

        //then finally notify the listener about the change
        if (changeListener != null)
            changeListener.accept(string);
    }


    // -- enums -- //


    public enum Filter {
        ANY(c -> true),
        LETTERS(c -> c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z'),
        NUMBERS(c -> c >= '0' && c <= '9'),
        ASCII(c -> c > 31 && c < 127);

        private final Predicate<Character> predicate;

        Filter(Predicate<Character> predicate) {
            this.predicate = predicate;
        }

        public boolean test(char c) {
            return predicate.test(c);
        }
    }

    private enum Action {
        WRITE,
        INSERT,
        DELETE,
        PASTE,
        UNDO
    }
}
