package cinnamon.gui.widgets.types;

import cinnamon.Client;
import cinnamon.gui.widgets.GUIListener;
import cinnamon.gui.widgets.SelectableWidget;
import cinnamon.gui.widgets.Tickable;
import cinnamon.model.GeometryHelper;
import cinnamon.render.Font;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.text.Style;
import cinnamon.text.Text;
import cinnamon.utils.*;

import java.util.function.Consumer;
import java.util.function.Predicate;

import static org.lwjgl.glfw.GLFW.*;

public class TextField extends SelectableWidget implements Tickable {

    private static final Resource TEXTURE = new Resource("textures/gui/widgets/text_field.png");
    private static final int
            CURSOR_WIDTH = 1,
            INSERT_WIDTH = 4,
            BLINK_SPEED = 20,
            HISTORY_SIZE = 20;
    private static final char
            PASSWORD_CHAR = '\u2022',
            FORMATTING_CHAR = '*';
    private static final Style HINT_STYLE = Style.EMPTY.italic(true).color(Colors.LIGHT_BLACK);
    private static final Predicate<Character> WORD_CHARACTERS = c -> Character.isAlphabetic(c) || Character.isDigit(c) || c == '_';

    private final Font font;
    private final ContextMenu contextMenu;

    //text
    private String currText = "";
    private int cursor;
    private boolean insert;
    private Consumer<String> changeListener;
    private Predicate<Character> filter = Filter.ANY.predicate;
    private int charLimit = 1000;
    private Consumer<TextField> enterListener;
    private int selectedIndex = -1;

    //mouse
    private long lastClickTime = -1;
    private int lastClickIndex = -1;
    private int clickCount;
    private boolean dragging;

    //history
    private final String[] history = new String[HISTORY_SIZE];
    private int historyIndex;
    private Action lastAction = null;

    //rendering
    private Text hintText = null;
    private Style style = Style.EMPTY;
    private Integer borderColor;
    private String formatting;
    private String formattedText = "";
    private boolean textOnly;
    private boolean password;
    private Integer selectionColor;
    private Integer selectedColor;
    private int xOffset;
    private float xAnim;
    private long blinkTime;

    public TextField(int x, int y, int width, int height, Font font) {
        super(x, y, width, height);
        this.font = font;

        contextMenu = new ContextMenu()
                .addAction(Text.of("Cut"), null, b -> cut())
                .addAction(Text.of("Copy"), null, b -> copy())
                .addAction(Text.of("Paste"), null, b -> paste())
                .addDivider()
                .addAction(Text.of("Select All"), null, b -> selectAll())
                .addDivider()
                .addAction(Text.of("Undo"), null, b -> undo())
                .addAction(Text.of("Redo"), null, b -> redo());
        contextMenu.setOpenListener(ctx -> updateContext());
        contextMenu.setForceFocusParent(true);
        this.setPopup(contextMenu);
    }


    // -- tick -- //


    @Override
    public void tick() {
        blinkTime++;
    }


    // -- rendering -- //


    @Override
    public void renderWidget(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        if (!textOnly)
            renderBackground(matrices, mouseX, mouseY, delta);

        UIHelper.pushScissors(getX() + 1, getY() + 1, getWidth() - 2, getHeight() - 2);
        matrices.push();

        renderText(matrices, mouseX, mouseY, delta);

        matrices.pop();
        UIHelper.popScissors();

        if (!textOnly)
            renderOverlay(matrices, mouseX, mouseY, delta);
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
        if (borderColor == null || (this.isHovered() && !this.isFocused()))
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
        int x = getX() + 2;
        int y = getCenterY() - Math.round(font.lineHeight * 0.5f);
        int height = Math.round(font.lineHeight) + 2;

        //hint text
        if (currText.isEmpty()) {
            if (hintText != null)
                font.render(VertexConsumer.FONT, matrices, x, y, hintText);

            //render cursor
            renderCursor(matrices, x, y - 1, height);
            xAnim = xOffset = 0;
            return;
        }

        //we have text, so here we go!
        int x0, x1 = x0 = x;
        int skipped = cursor + getFormattingSkippedCharCount(cursor);
        String str = getFormattedText();
        Text text;

        //offset x0 based on the cursor
        if (cursor > 0) {
            Text index = Text.of(str.substring(0, skipped)).withStyle(style);
            x0 += TextUtils.getWidth(index, font);
        }

        //offset x1 based on the selected index
        //also generate the text with the style, but with inverted colors for the selection
        if (selectedIndex != -1) {
            //x1 offset
            int extra = selectedIndex + getFormattingSkippedCharCount(selectedIndex);
            Text index = Text.of(str.substring(0, extra)).withStyle(style);
            x1 += TextUtils.getWidth(index, font);

            //text
            int start = Math.min(skipped, extra);
            int end = Math.max(skipped, extra);
            int color = selectedColor == null ? Colors.BLACK.rgba : selectedColor;

            text = Text.empty().withStyle(style)
                    .append(Text.of(str.substring(0, start)))
                    .append(Text.of(str.substring(start, end)).withStyle(style.color(color).background(false)))
                    .append(Text.of(str.substring(end)));
        } else {
            //no selection, so just use the text
            text = Text.of(str).withStyle(style);
        }

        //if the text is too large, we may need to offset it
        int cursorX = x0 + xOffset;
        int w = getWidth();

        //fit the cursor inside the scissors, taking into account the x offset
        if (cursorX < x) {
            xOffset += x - cursorX;
        } else if (cursorX > x + w - 4 - 4) {
            xOffset -= cursorX - (x + w - 4 - 4);
        }

        //translate offset based on the remaining empty space, if any
        if (xOffset < 0) {
            int emptySpace = Math.min(TextUtils.getWidth(text, font) + 2 + xOffset - w + 2 + 4, 0);
            xOffset = Math.min(0, xOffset - emptySpace);
        }

        //smooth and apply the offset
        float d = UIHelper.tickDelta(0.4f);
        xAnim = Maths.lerp(xAnim, xOffset, d);
        matrices.translate(xAnim, 0, 0);

        //render text
        font.render(VertexConsumer.FONT, matrices, x, y, text);
        //render selection
        renderSelection(matrices, x0, x1, y - 1, height);
        //render cursor
        renderCursor(matrices, x0, y - 1, height);
    }

    protected void renderSelection(MatrixStack matrices, float x0, float x1, float y, float height) {
        if (selectedIndex == -1)
            return;
        float t = x0;
        x0 = Math.min(x0, x1);
        x1 = Math.max(t, x1);
        GeometryHelper.rectangle(VertexConsumer.GUI, matrices, x0, y, x1, y + height, selectionColor == null ? UIHelper.ACCENT.rgba : selectionColor);
    }

    protected void renderCursor(MatrixStack matrices, float x, float y, float height) {
        if (isFocused() && blinkTime % BLINK_SPEED < BLINK_SPEED / 2) {
            matrices.push();
            //translate matrices so we can render on top of text
            matrices.translate(0, 0, Font.Z_DEPTH);
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
        updateFormatting();
    }

    public void setFormatting(String formatting) {
        this.formatting = formatting;
        updateFormatting();
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

    public void setSelectionColor(Colors color) {
        setSelectionColor(color.rgba);
    }

    public void setSelectionColor(Integer color) {
        this.selectionColor = color;
    }

    public void setSelectedTextColor(Colors color) {
        setSelectedTextColor(color.rgba);
    }

    public void setSelectedTextColor(Integer color) {
        this.selectedColor = color;
    }

    public void setCursorPos(int cursor) {
        this.cursor = Math.min(Math.max(cursor, 0), currText.length());
        this.blinkTime = 0;
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

            if (c == FORMATTING_CHAR) {
                build.append(password ? PASSWORD_CHAR : s.charAt(chars));
                chars++;
                continue;
            }

            build.append(c);
        }

        if (chars < length) {
            String str = s.substring(chars);
            build.append(password ? String.valueOf(PASSWORD_CHAR).repeat(str.length()) : str);
        }

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

            if (c == FORMATTING_CHAR) {
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
            if (escaped || c != FORMATTING_CHAR) {
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
        boolean shift = (mods & GLFW_MOD_SHIFT) != 0;

        switch (key) {
            //navigation
            case GLFW_KEY_LEFT -> {
                markSelection(shift);
                if (ctrl) { //move word
                    setCursorPos(getPreviousWord(cursor));
                } else { //move char
                    setCursorPos(cursor - 1);
                    if (isSurrogate(cursor))
                        setCursorPos(cursor - 1);
                }
                return this;
            }
            case GLFW_KEY_RIGHT -> {
                markSelection(shift);
                if (ctrl) { //move word
                    setCursorPos(getNextWord(cursor));
                } else { //move char
                    setCursorPos(cursor + 1);
                    if (isSurrogate(cursor))
                        setCursorPos(cursor + 1);
                }
                return this;
            }
            case GLFW_KEY_HOME, GLFW_KEY_PAGE_UP -> {
                markSelection(shift);
                //no lines, so page up is the same as home
                setCursorPos(0);
                return this;
            }
            case GLFW_KEY_END, GLFW_KEY_PAGE_DOWN -> {
                markSelection(shift);
                //same for page down
                setCursorPos(currText.length());
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
                    paste();
                    return this;
                }
            }
            case GLFW_KEY_Z -> {
                if (ctrl) {
                    if (shift) redo();
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
            case GLFW_KEY_A -> {
                if (ctrl) {
                    selectAll();
                    return this;
                }
            }
            case GLFW_KEY_C -> {
                if (ctrl) {
                    copy();
                    return this;
                }
            }
            case GLFW_KEY_X -> {
                if (ctrl) {
                    cut();
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

        if (insert) insert(c);
        else append(String.valueOf(c), Action.WRITE);
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

    private boolean isSurrogate(int cursor) {
        return cursor >= 0 && cursor < currText.length() && Character.isLowSurrogate(currText.charAt(cursor));
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

    private void markSelection(boolean shift) {
        if (!shift) //shift is not pressed, so reset the selection
            selectedIndex = -1;
        else if (selectedIndex == -1)
            selectedIndex = cursor;
    }

    private void selectAll() {
        setCursorPos(currText.length());
        selectedIndex = 0;
    }

    private void copy() {
        if (selectedIndex != -1 && !password)
            glfwSetClipboardString(-1, currText.substring(Math.min(cursor, selectedIndex), Math.max(cursor, selectedIndex)));
    }

    private void paste() {
        String clipboard = glfwGetClipboardString(-1);
        if (clipboard != null) {
            //no new lines allowed
            append(clipboard.replaceAll("(\r\n)|\n", " "), Action.PASTE);
        }
    }

    private void cut() {
        if (selectedIndex != -1 && !password) {
            glfwSetClipboardString(-1, currText.substring(Math.min(cursor, selectedIndex), Math.max(cursor, selectedIndex)));
            setText(removeSelected(Action.CUT));
        }
    }

    private void undo() {
        //cannot undo from here
        if (historyIndex <= 0)
            return;

        //save the undo action, however it may increase the index, so we need to save it beforehand
        int i = historyIndex - 1;
        appendToHistory(Action.UNDO);

        //save the old index and set the text
        historyIndex = i;
        lastAction = null;
        setFromHistory();
    }

    private void redo() {
        //no redo available
        if (historyIndex >= history.length - 1 || history[historyIndex + 1] == null)
            return;

        //increase the history index and set the text
        historyIndex++;
        lastAction = null;
        setFromHistory();
    }

    private void appendToHistory(Action action) {
        //if it is the same action, we do not want to save it
        if (lastAction == action)
            return;

        //store the current action and add the cursor positions to the text, then save the text
        lastAction = action;
        String text = cursor + "|" + selectedIndex + "|" + currText;
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

    private void setFromHistory() {
        //unwrap the text and cursor position
        String text = history[historyIndex];
        String[] split = text.split("\\|", 3);

        //set the cursor and text
        setText(split[2]);
        selectedIndex = Integer.parseInt(split[1]);
        setCursorPos(Integer.parseInt(split[0]));
    }

    private void insert(char c) {
        //if the cursor is at the end, we can just append the char
        if (cursor == currText.length()) {
            append(String.valueOf(c), Action.INSERT);
            return;
        }

        //otherwise we test the char, then substring it inside the text
        if (filter.test(c)) {
            //get tne text removing, if any, selected text
            String text = removeSelected(Action.WRITE_SEL);
            //if the text was modified, we set the action to this
            if (!text.equals(currText))
                lastAction = Action.INSERT;

            //backup the text
            appendToHistory(Action.INSERT);

            //check for surrogates
            int i, j = i = cursor + 1;
            if (isSurrogate(i)) i++;

            //set the new text and cursor pos
            setText(text.substring(0, cursor) + c + text.substring(i));
            setCursorPos(j);
        }
    }

    private void append(String s, Action action) {
        StringBuilder build = new StringBuilder();

        //test each char and append it to the build
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (filter.test(c))
                build.append(c);
        }

        //append the build to the text, caring for the cursor position
        if (!build.isEmpty()) {
            //get tne text removing, if any, selected text
            String text = removeSelected(Action.WRITE_SEL);
            //if the text was modified, we set the action to this
            if (!text.equals(currText))
                lastAction = action;

            //then backup the text, change cursor and set the new text
            appendToHistory(action);
            setText(text.substring(0, cursor) + build + text.substring(cursor));
            setCursorPos(cursor + s.length());
        }
    }

    private void remove(int count) {
        int len = currText.length();
        if (len == 0) //nothing to remove
            return;

        //remove the selected text and void the remove event
        String text = removeSelected(Action.DELETE);
        if (!text.equals(currText)) {
            setText(text);
            return;
        }

        //history
        appendToHistory(Action.DELETE);

        //negative count means delete forward
        if (count < 0) {
            if (cursor < len) {
                //get the substring position
                int i = cursor - count;
                //check for surrogates
                if (isSurrogate(i)) i++;
                //set the new substring text
                setText(currText.substring(0, cursor) + currText.substring(Math.min(i, len)));
            }
        } else {
            if (cursor > 0) {
                //get the substring position
                int i = cursor - count;
                //check for surrogates
                if (isSurrogate(i)) i--;
                //set the new substring text
                setText(currText.substring(0, Math.max(i, 0)) + currText.substring(cursor));
                //also set the new cursor position
                setCursorPos(i);
            }
        }
    }

    private void moveCursorToMouse() {
        //rendering text offset
        int x = getX() + 2;
        //grab mouse pos
        int mousePos = Client.getInstance().window.mouseX;
        //get the text
        Text text = Text.of(getFormattedText()).withStyle(style);
        //convert the mouse pos to the text space and get the length at the position
        Text clamped = font.clampToWidth(text, mousePos - xOffset - x, true);
        //grab the length of the text
        int length = clamped.asString().length();
        //since the length uses the formatting, we need to subtract the extra chars
        setCursorPos(length - getFormattingExtraCharCount(length));
    }

    private String removeSelected(Action context) {
        //cannot remove if there is no selection, so return the current text
        if (selectedIndex == -1)
            return currText;

        //fix positions
        int start = Math.min(cursor, selectedIndex);
        int end = Math.max(cursor, selectedIndex);

        //save to history and update the cursor
        appendToHistory(context);
        setCursorPos(start);
        selectedIndex = -1;

        //return the new modified text
        return currText.substring(0, start) + currText.substring(end);
    }

    private void updateContext() {
        //0, 1 - cut / copy
        contextMenu.getAction(0).setActive(selectedIndex != -1 && !password);
        contextMenu.getAction(1).setActive(selectedIndex != -1 && !password);
        //4, 5 - undo / redo
        contextMenu.getAction(4).setActive(historyIndex > 0);
        contextMenu.getAction(5).setActive(historyIndex < history.length - 1 && history[historyIndex + 1] != null);
    }

    private void updateFormatting() {
        if (formatting != null)
            formattedText = applyFormatting(currText);
        else if (password)
            formattedText = String.valueOf(PASSWORD_CHAR).repeat(currText.length());
        else
            formattedText = currText;
    }

    @Override
    public GUIListener mousePress(int button, int action, int mods) {
        if (!isActive() || !isHovered() || action != GLFW_PRESS || button != GLFW_MOUSE_BUTTON_1) {
            dragging = false;
            return super.mousePress(button, action, mods);
        }

        //flags
        UIHelper.focusWidget(this);
        dragging = true;

        //shift selects the text
        if ((mods & GLFW_MOD_SHIFT) != 0) {
            if (selectedIndex == -1)
                selectedIndex = cursor;
            moveCursorToMouse();
            clickCount = 0;
            return this;
        }

        //move the cursor to the mouse position
        moveCursorToMouse();

        //click count
        long now = Client.getInstance().ticks;
        if (clickCount != 0 && lastClickIndex == cursor && now - lastClickTime < UIHelper.DOUBLE_CLICK_TIME)
            clickCount++;
        else
            clickCount = 1;

        //save variables
        selectedIndex = -1;
        lastClickTime = now;
        lastClickIndex = cursor;

        //extra click actions
        switch (clickCount) {
            case 2 -> {
                if (cursor >= currText.length()) {
                    selectAll();
                    clickCount = 0; //reset actions
                } else {
                    //do not select if the cursor - 1 is a whitespace nor the next char
                    selectedIndex = cursor == 0 || Character.isWhitespace(currText.charAt(cursor - 1)) ? cursor : getPreviousWord(cursor);
                    if (!Character.isWhitespace(currText.charAt(cursor)))
                        setCursorPos(getNextWord(cursor));
                }
            }
            case 3 -> {
                selectAll();
                clickCount = 0; //reset actions
            }
        }

        return this;
    }

    @Override
    public GUIListener mouseMove(int x, int y) {
        if (!dragging)
            return super.mouseMove(x, y);

        //start selection
        if (selectedIndex == -1)
            selectedIndex = cursor;

        //move the cursor to the mouse position
        moveCursorToMouse();

        return this;
    }

    public void setText(String string) {
        //trim the text to the char limit
        if (string.length() > charLimit)
            string = string.substring(0, charLimit);

        //do not update if the text is the same
        if (currText.equals(string))
            return;

        //set the text and update cursor to fit the text bounds
        currText = string;
        setCursorPos(cursor);

        //update the formatted text
        updateFormatting();

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
    }

    private enum Action {
        WRITE,
        INSERT,
        DELETE,
        PASTE,
        UNDO,
        CUT,
        WRITE_SEL
    }
}

