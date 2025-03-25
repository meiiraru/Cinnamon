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
import cinnamon.utils.Colors;
import cinnamon.utils.Maths;
import cinnamon.utils.TextUtils;
import cinnamon.utils.UIHelper;

import java.util.function.Consumer;
import java.util.function.Predicate;

import static org.lwjgl.glfw.GLFW.*;

public class TextField extends SelectableWidget implements Tickable {

    public static final int
            HISTORY_SIZE = 20,
            DRAG_ZONE = 8;
    public static final char
            FORMATTING_CHAR = '*';
    public static final Predicate<Character> WORD_CHARACTERS = c -> Character.isAlphabetic(c) || Character.isDigit(c) || c == '_';

    protected final ContextMenu contextMenu;

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
    private Style textStyle = Style.EMPTY;
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

    public TextField(int x, int y, int width, int height) {
        super(x, y, width, height);

        contextMenu = new ContextMenu()
                .addAction(Text.translated("gui.text_field.cut"), null, b -> cut())
                .addAction(Text.translated("gui.text_field.copy"), null, b -> copy())
                .addAction(Text.translated("gui.text_field.paste"), null, b -> paste())
                .addDivider()
                .addAction(Text.translated("gui.text_field.select_all"), null, b -> selectAll())
                .addDivider()
                .addAction(Text.translated("gui.text_field.undo"), null, b -> undo())
                .addAction(Text.translated("gui.text_field.redo"), null, b -> redo());
        contextMenu.setOpenListener(ctx -> updateContext());
        contextMenu.setForceFocusParent(true);
        this.setPopup(contextMenu);
    }


    // -- tick -- //


    @Override
    public void tick() {
        //tick the cursor blink
        blinkTime++;

        //move the text when dragging
        if (dragging) {
            int w = getWidth();
            int tw = TextUtils.getWidth(Text.of(getFormattedText()).withStyle(Style.EMPTY.guiStyle(getStyleRes()).applyParent(textStyle)));

            //only move if the text is bigger than the width
            if (tw > w - DRAG_ZONE * 2) {
                int x = getX();
                int mx = Client.getInstance().window.mouseX;

                //dragging to the left
                if (mx < x + DRAG_ZONE) {
                    //get speed based on distance from dead zone, then apply to the offset
                    int speed = x + DRAG_ZONE - mx;
                    xOffset = Math.min(xOffset + speed, 0);
                    //reselect the text as were dragging
                    selectOnMousePos();
                }
                //dragging to the right
                else if (mx > x + w - DRAG_ZONE) {
                    int speed = mx - x - w + DRAG_ZONE;
                    xOffset = Math.max(xOffset - speed, w - 4 * 2 - tw);
                    selectOnMousePos();
                }
            }
        }
    }


    // -- rendering -- //


    @Override
    public void renderWidget(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        if (!textOnly)
            renderBackground(matrices, mouseX, mouseY, delta);

        UIHelper.pushScissors(getX() + 1, getY() + 1, getWidth() - 2, getHeight() - 2);
        matrices.push();

        //smooth and apply the offset
        float d = UIHelper.tickDelta(0.4f);
        xAnim = Maths.lerp(xAnim, xOffset, d);
        matrices.translate(xAnim, 0, 0);

        if (!textOnly)
            matrices.translate(0, 0, UIHelper.getDepthOffset());
        renderText(matrices, mouseX, mouseY, delta);

        matrices.pop();
        UIHelper.popScissors();

        if (!textOnly)
            renderOverlay(matrices, mouseX, mouseY, delta);
    }

    protected void renderBackground(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        UIHelper.nineQuad(
                VertexConsumer.GUI, matrices, getStyle().textFieldTex,
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

        matrices.push();
        matrices.translate(0, 0, UIHelper.getDepthOffset());
        UIHelper.nineQuad(
                VertexConsumer.GUI, matrices, getStyle().textFieldTex,
                getX(), getY(),
                getWidth(), getHeight(),
                48f, 0f,
                16, 16,
                64, 16,
                borderColor
        );
        matrices.pop();
    }

    protected void renderText(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        int x = getX() + 2;
        int y = getCenterY() - Math.round(getStyle().font.lineHeight * 0.5f);
        int height = Math.round(getStyle().font.lineHeight) + 2;

        //hint text
        if (currText.isEmpty()) {
            if (hintText != null)
                Text.empty()
                        .withStyle(Style.EMPTY
                                .italic(true)
                                .color(isActive() ? getStyle().hintColor : getStyle().disabledHintColor)
                                .guiStyle(getStyleRes()))
                        .append(hintText)
                        .render(VertexConsumer.FONT, matrices, x, y);

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
        Style textStyle = Style.EMPTY.guiStyle(getStyleRes()).applyParent(this.textStyle);

        //offset x0 based on the cursor
        if (cursor > 0) {
            Text index = Text.of(str.substring(0, skipped)).withStyle(textStyle);
            x0 += TextUtils.getWidth(index);
        }

        //render cursor
        renderCursor(matrices, x0, y - 1, height);

        //offset x1 based on the selected index
        //also generate the text with the style, but with inverted colors for the selection
        if (selectedIndex != -1 && cursor != selectedIndex) {
            //x1 offset
            int extra = selectedIndex + getFormattingSkippedCharCount(selectedIndex);
            Text index = Text.of(str.substring(0, extra)).withStyle(textStyle);
            x1 += TextUtils.getWidth(index);

            //render selection
            renderSelection(matrices, x0, x1, y - 1, height);
            matrices.translate(0, 0, UIHelper.getDepthOffset());

            //text
            int start = Math.min(skipped, extra);
            int end = Math.max(skipped, extra);
            int color = selectedColor == null ? getStyle().selectedTextColor : selectedColor;

            text = Text.empty().withStyle(textStyle)
                    .append(Text.of(str.substring(0, start)))
                    .append(Text.of(str.substring(start, end)).withStyle(Style.EMPTY.color(color).background(false).outlined(false).shadow(false)))
                    .append(Text.of(str.substring(end)));
        } else {
            //no selection, so just use the text
            text = Text.of(str).withStyle(textStyle);
        }

        //render text
        text.render(VertexConsumer.FONT, matrices, x, y);
    }

    protected void renderCursor(MatrixStack matrices, float x, float y, float height) {
        if (isActive() && isFocused() && blinkTime % getStyle().blinkSpeed < getStyle().blinkSpeed / 2) {
            matrices.push();
            matrices.translate(0, 0, UIHelper.getDepthOffset() * (Font.Z_DEPTH + 2));
            VertexConsumer.GUI.consume(GeometryHelper.rectangle(matrices, x, y, x + (insert ? getStyle().insertWidth : getStyle().cursorWidth), y + height, borderColor == null ? 0xFFFFFFFF : borderColor));
            matrices.pop();
        }
    }

    protected void renderSelection(MatrixStack matrices, float x0, float x1, float y, float height) {
        float t = x0;
        x0 = Math.min(x0, x1);
        x1 = Math.max(t, x1);
        VertexConsumer.GUI.consume(GeometryHelper.rectangle(matrices, x0, y, x1, y + height, selectionColor == null ? getStyle().accentColor : selectionColor));
    }


    // -- getters and setters -- //


    public void setHintText(Text hintText) {
        this.hintText = hintText;
    }

    public void setTextStyle(Style style) {
        this.textStyle = style == null ? Style.EMPTY : style;
    }

    public Style getTextStyle() {
        return textStyle;
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
                build.append(password ? getStyle().passwordChar : s.charAt(chars));
                chars++;
                continue;
            }

            build.append(c);
        }

        if (chars < length) {
            String str = s.substring(chars);
            build.append(password ? String.valueOf(getStyle().passwordChar).repeat(str.length()) : str);
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

    private void updateFormatting() {
        if (formatting != null)
            formattedText = applyFormatting(currText);
        else if (password)
            formattedText = String.valueOf(getStyle().passwordChar).repeat(currText.length());
        else
            formattedText = currText;
    }

    private void updateContext() {
        //0, 1 - cut / copy
        boolean bool = selectedIndex != -1 && selectedIndex != cursor && !password;
        contextMenu.getAction(0).setActive(bool);
        contextMenu.getAction(1).setActive(bool);
        //4, 5 - undo / redo
        contextMenu.getAction(4).setActive(historyIndex > 0);
        contextMenu.getAction(5).setActive(historyIndex < history.length - 1 && history[historyIndex + 1] != null);
    }

    private void fitCursorInWidth() {
        String str = getFormattedText();
        int width = getWidth() - 4 * 2;

        //if the text is too big, we try to fit it in the remaining space
        Style textStyle = Style.EMPTY.guiStyle(getStyleRes()).applyParent(this.textStyle);
        int textWidth = TextUtils.getWidth(Text.of(str).withStyle(textStyle));
        if (textWidth > width) {
            int remainingSpace = width - (textWidth + xOffset);
            if (remainingSpace > 0)
                xOffset += remainingSpace;
        } else {
            xOffset = 0;
        }

        //cursor is inside the boundaries - nothing to fit
        int cursorX = TextUtils.getWidth(Text.of(str.substring(0, cursor)).withStyle(textStyle)) + xOffset;
        if (cursorX >= 0 && cursorX < width)
            return;

        //cursor too much to the left
        if (cursorX < 0) {
            xOffset = -cursorX + xOffset;
            return;
        }

        //cursor too much to the right
        xOffset += width - cursorX;
    }


    // -- events -- //


    @Override
    public GUIListener keyPress(int key, int scancode, int action, int mods) {
        if (!isActive() || !isFocused() || action == GLFW_RELEASE)
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
                fitCursorInWidth();
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
                fitCursorInWidth();
                return this;
            }
            case GLFW_KEY_HOME, GLFW_KEY_PAGE_UP -> {
                markSelection(shift);
                //no lines, so page up is the same as home
                setCursorPos(0);
                fitCursorInWidth();
                return this;
            }
            case GLFW_KEY_END, GLFW_KEY_PAGE_DOWN -> {
                markSelection(shift);
                //same for page down
                setCursorPos(currText.length());
                fitCursorInWidth();
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
        if (!isActive() || !isFocused())
            return super.charTyped(c, mods);

        if (insert) insert(c);
        else append(String.valueOf(c), Action.WRITE);
        return this;
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
        if (clickCount == 0 || (lastClickIndex == cursor && now - lastClickTime < getStyle().doubleClickDelay))
            clickCount++;
        else
            clickCount = 1;

        //save variables
        lastClickTime = now;
        lastClickIndex = cursor;

        //extra click actions
        switch (clickCount) {
            case 1 -> selectedIndex = -1;
            case 2 -> {
                //no words in passwords
                if (password) {
                    selectAll();
                    //select whitespaces
                } else if (isWhitespace(cursor - 1) && isWhitespace(cursor)) {
                    selectWhitespaces(cursor);
                    //select word
                } else {
                    selectWord(cursor);
                }
            }
            //3+
            default -> selectAll();
        }

        return this;
    }

    @Override
    public GUIListener mouseMove(int x, int y) {
        if (dragging) {
            selectOnMousePos();
            return this;
        }

        return super.mouseMove(x, y);
    }


    // -- text editing -- //


    public void setText(String string) {
        //trim the text to the char limit
        if (string.length() > charLimit)
            string = string.substring(0, charLimit);

        //do not update if the text is the same
        if (currText.equals(string))
            return;

        //update the text
        currText = string;
        updateFormatting();

        //update the cursor
        setCursorPos(cursor);
        fitCursorInWidth();

        //then finally notify the listener about the change
        if (changeListener != null)
            changeListener.accept(string);
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
            fitCursorInWidth();
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
            fitCursorInWidth();
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
                fitCursorInWidth();
            }
        }
    }

    private boolean isSurrogate(int cursor) {
        return cursor >= 0 && cursor < currText.length() && Character.isLowSurrogate(currText.charAt(cursor));
    }

    private boolean isWhitespace(int cursor) {
        //consider out of bounds as whitespace
        return cursor < 0 || cursor >= currText.length() || Character.isWhitespace(currText.charAt(cursor));
    }


    // -- history -- //


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
        //if it is the same action, we do not want to save it, unless if it is a paste action
        if (lastAction == action && action != Action.PASTE)
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
        fitCursorInWidth();
    }


    // -- selection -- //


    private int getPreviousWord(int i) {
        if (password) //for security, passwords do not have words
            return 0;

        //skip spaces
        while (i > 0 && isWhitespace(i - 1))
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
        while (i < len && isWhitespace(i))
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

    private void selectAll() {
        setCursorPos(currText.length());
        selectedIndex = 0;
    }

    private void selectWhitespaces(int i) {
        //no words in passwords
        if (password)
            return;

        //spaces to the left
        selectedIndex = i;
        while (selectedIndex > 0 && isWhitespace(selectedIndex - 1))
            selectedIndex--;

        //spaces to the right
        int len = currText.length();
        while (i < len && isWhitespace(i))
            i++;
        setCursorPos(i);
    }

    private void selectWord(int i) {
        //no words in passwords
        if (password)
            return;

        //set the word positions
        selectedIndex = isWhitespace(i - 1) ? i : getPreviousWord(i);
        setCursorPos(getNextWord(selectedIndex));
    }

    private void markSelection(boolean shift) {
        if (!shift) //shift is not pressed, so reset the selection
            selectedIndex = -1;
        else if (selectedIndex == -1)
            selectedIndex = cursor;
    }

    private void moveCursorToMouse() {
        //rendering text offset
        int x = getX() + 2;
        //grab mouse pos
        int mousePos = Client.getInstance().window.mouseX;
        //get the text
        Text text = Text.of(getFormattedText()).withStyle(Style.EMPTY.guiStyle(getStyleRes()).applyParent(textStyle));
        //convert the mouse pos to the text space and get the length at the position
        Text clamped = getStyle().font.clampToWidth(text, mousePos - xOffset - x, true);
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

    private void selectOnMousePos() {
        //start selection
        if (selectedIndex == -1)
            selectedIndex = cursor;

        //move the cursor to the mouse position
        int oldCursor = cursor;
        moveCursorToMouse();

        //move words
        if (!password && clickCount == 2 && oldCursor != cursor) {
            int newCursor;

            //were selecting the right side
            if (oldCursor > selectedIndex) {
                //invert
                if (cursor < selectedIndex) {
                    selectedIndex = oldCursor;
                    newCursor = getPreviousWord(cursor);
                } else {
                    newCursor = getNextWord(cursor);
                }
            }
            //left side
            else {
                //invert
                if (cursor > selectedIndex) {
                    selectedIndex = oldCursor;
                    newCursor = getNextWord(cursor);
                } else {
                    newCursor = getPreviousWord(cursor);
                }
            }

            //update cursor position
            setCursorPos(newCursor);
        }
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

