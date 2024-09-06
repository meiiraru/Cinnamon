package cinnamon.gui.widgets.types;

import cinnamon.Client;
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

public class SelectionBox extends Button {

    private final List<Text> indexes = new ArrayList<>();

    protected Text selectedText = Text.of("-");
    protected int selected = -1;

    protected Consumer<Integer> changeListener;
    protected boolean closeOnSelect = true;

    public SelectionBox(int x, int y, int width, int height) {
        super(x, y, width, height, Text.of(""), button -> {
            SelectionBox box = (SelectionBox) button;
            box.openPopup(box.getX(), box.getY() + box.getHeight());
        });

        ContextMenu ctx = new ContextMenu(width, height) {
            @Override
            public GUIListener mousePress(int button, int action, int mods) {
                boolean wasOpen = isOpen();
                GUIListener sup = super.mousePress(button, action, mods);
                return sup == null && wasOpen && SelectionBox.this.isHovered() ? this : sup;
            }
        };
        super.setPopup(ctx);
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
        int y = getCenterY() - TextUtils.getHeight(text, f) / 2;
        f.render(VertexConsumer.FONT, matrices, x, y, text);

        //render selected text
        text = TextUtils.addEllipsis(selectedText, f, getWidth() - width - 4);
        x = getX() + 2;
        y = getCenterY() - TextUtils.getHeight(text, f) / 2;
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

    public SelectionBox setChangeListener(Consumer<Integer> action) {
        this.changeListener = action;
        return this;
    }

    public SelectionBox closeOnSelect(boolean bool) {
        this.closeOnSelect = bool;
        return this;
    }

    public SelectionBox addEntry(Text name) {
        return addEntry(name, null);
    }

    public SelectionBox addEntry(Text name, Text tooltip) {
        return addEntry(name, tooltip, null);
    }

    public SelectionBox addEntry(Text name, Text tooltip, Consumer<Button> action) {
        int index = indexes.size();
        indexes.add(name);

        ((ContextMenu) getPopup()).addAction(name, tooltip, button -> {
            select(index);
            if (action != null)
                action.accept(button);
        });

        return this;
    }

    public SelectionBox addDivider() {
        ((ContextMenu) getPopup()).addDivider();
        return this;
    }

    /*
    public SelectionBox addSubMenu(Text name, ContextMenu subMenu) {
        ((ContextMenu) getPopup()).addSubMenu(name, subMenu);
        return this;
    }
    */

    private void updateTexts() {
        for (int i = 0; i < indexes.size(); i++) {
            //get text
            Text text = indexes.get(i);

            //selected entry
            if (i == selected)
                text = Text.empty().withStyle(Style.EMPTY.color(UIHelper.ACCENT)).append(text);

            //apply text
            ((ContextMenu) getPopup()).getAction(i).setMessage(text);
        }
    }

    @Override
    protected void openPopup(int x, int y) {
        PopupWidget popup = getPopup();
        UIHelper.setPopup(x, y, popup);
        popup.open();
    }

    @Override
    public GUIListener scroll(double x, double y) {
        if (UIHelper.isWidgetHovered(this)) {
            int i = selected;
            i += (int) Math.signum(-y);
            i = Maths.modulo(i, indexes.size());
            select(i);
            ((ContextMenu) getPopup()).getAction(i).onRun();
            return this;
        }

        return super.scroll(x, y);
    }
}
