package cinnamon.gui.widgets.types;

import cinnamon.Client;
import cinnamon.gui.GUIStyle;
import cinnamon.gui.widgets.GUIListener;
import cinnamon.gui.widgets.PopupWidget;
import cinnamon.render.Font;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.text.Style;
import cinnamon.text.Text;
import cinnamon.utils.Maths;
import cinnamon.utils.TextUtils;
import cinnamon.utils.UIHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.lwjgl.glfw.GLFW.GLFW_PRESS;

public class ComboBox extends Button {

    private final List<Text> indexes = new ArrayList<>();

    protected Text selectedText = Text.of("-");
    protected int selected = -1;

    protected Consumer<Integer> changeListener;
    protected boolean closeOnSelect = true;

    private final ContextMenu contextMenu, emptyMenu;

    public ComboBox(int x, int y, int width, int height) {
        super(x, y, width, height, Text.of(""), button -> {
            if (button.getPopup().isOpen())
                button.getPopup().close();
            else
                ((ComboBox) button).openPopup(button.getX(), button.getY() + button.getHeight());
        });

        contextMenu = new ComboContext(width, height, this);
        emptyMenu = new ComboContext(width, height, this);
        emptyMenu.addAction(Text.of("-"), null, null);

        super.setPopup(emptyMenu);
    }

    @Override
    public void setPopup(PopupWidget popup) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void renderText(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        Font f = Client.getInstance().font;

        //render arrow
        Text text = Text.of(isExpanded() ? "\u23F6" : "\u23F7");
        int width = TextUtils.getWidth(text, f);
        int x = getX() + getWidth() - width - 2;
        int y = getCenterY() - TextUtils.getHeight(text, f) / 2 + (isHolding() ? GUIStyle.pressYOffset : 0);
        f.render(VertexConsumer.FONT, matrices, x, y, text);

        //render selected text
        text = TextUtils.addEllipsis(selectedText, f, getWidth() - width - 4);
        x = getX() + 2;
        f.render(VertexConsumer.FONT, matrices, x, y, text);
    }

    public boolean isExpanded() {
        return getPopup().isOpen();
    }

    public int getSelectedIndex() {
        return selected;
    }

    public void select(int index) {
        this.selected = index;
        this.selectedText = indexes.get(index);
        if (changeListener != null)
            changeListener.accept(index);
        if (closeOnSelect)
            getPopup().close();
        updateTexts();
    }

    public ComboBox setChangeListener(Consumer<Integer> action) {
        this.changeListener = action;
        return this;
    }

    public ComboBox closeOnSelect(boolean bool) {
        this.closeOnSelect = bool;
        return this;
    }

    public ComboBox addEntry(Text name) {
        return addEntry(name, null);
    }

    public ComboBox addEntry(Text name, Text tooltip) {
        return addEntry(name, tooltip, null);
    }

    public ComboBox addEntry(Text name, Text tooltip, Consumer<Button> action) {
        int index = indexes.size();
        indexes.add(name);

        contextMenu.addAction(name, tooltip, button -> {
            select(index);
            if (action != null)
                action.accept(button);
        });

        super.setPopup(contextMenu);
        return this;
    }

    public ComboBox addDivider() {
        contextMenu.addDivider();
        super.setPopup(contextMenu);
        return this;
    }

    /*
    public ComboBox addSubMenu(Text name, ContextMenu subMenu) {
        contextMenu.addSubMenu(name, subMenu);
        super.setPopup(contextMenu);
        return this;
    }
    */

    public void clearEntries() {
        contextMenu.clearActions();
        super.setPopup(emptyMenu);

        indexes.clear();
        selectedText = Text.of("-");
        selected = -1;
    }

    private void updateTexts() {
        for (int i = 0; i < indexes.size(); i++) {
            //get text
            Text text = indexes.get(i);

            //selected entry
            if (i == selected)
                text = Text.empty().withStyle(Style.EMPTY.color(GUIStyle.accentColor)).append(text);

            //apply text
            contextMenu.getAction(i).setMessage(text);
        }
    }

    @Override
    public GUIListener scroll(double x, double y) {
        if (UIHelper.isWidgetHovered(this)) {
            int i = selected;
            i += (int) Math.signum(-y);
            i = Maths.modulo(i, indexes.size());
            select(i);
            contextMenu.getAction(i).onRun();
            return this;
        }

        return super.scroll(x, y);
    }

    private static class ComboContext extends ContextMenu {
        private final ComboBox parent;

        public ComboContext(int width, int height, ComboBox parent) {
            super(width, height);
            this.parent = parent;
        }

        @Override
        public GUIListener mousePress(int button, int action, int mods) {
            if (parent.isHolding() || (!UIHelper.isWidgetHovered(this) && UIHelper.isWidgetHovered(parent) && this.isOpen() && action == GLFW_PRESS))
                return null;
            return super.mousePress(button, action, mods);
        }
    }
}
