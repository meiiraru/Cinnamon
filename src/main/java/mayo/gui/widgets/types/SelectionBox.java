package mayo.gui.widgets.types;

import mayo.Client;
import mayo.gui.widgets.GUIListener;
import mayo.render.Font;
import mayo.render.MatrixStack;
import mayo.render.batch.VertexConsumer;
import mayo.text.Style;
import mayo.text.Text;
import mayo.utils.Maths;
import mayo.utils.TextUtils;
import mayo.utils.UIHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class SelectionBox extends Button {

    private final List<Text> indexes = new ArrayList<>();

    protected Text selectedText = Text.of("-");
    protected int selected = -1;
    protected Consumer<Integer> changeListener;
    protected boolean expanded;

    protected boolean closeOnSelect;

    public SelectionBox(int x, int y, int width, int height) {
        super(x, y, width, height, Text.of(""), button -> {
            SelectionBox box = (SelectionBox) button;
            if (box.expanded) box.getContextMenu().close();
            else box.openContext(box.getX(), box.getY() + box.getHeight());
            box.expanded = !box.expanded;
        });

        super.setContextMenu(new ContextMenu(width, height));
    }

    @Override
    public void setContextMenu(ContextMenu contextMenu) {
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
        return expanded;
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
            getContextMenu().close();
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

        getContextMenu().addAction(name, tooltip, button -> {
            select(index);
            if (action != null)
                action.accept(button);
        });

        return this;
    }

    public SelectionBox addDivider() {
        getContextMenu().addDivider();
        return this;
    }

    /*
    public SelectionBox addSubMenu(Text name, ContextMenu subMenu) {
        getContextMenu().addSubMenu(name, subMenu);
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
            getContextMenu().getAction(i).setMessage(text);
        }
    }

    @Override
    protected void openContext(int x, int y) {
        ContextMenu context = getContextMenu();
        UIHelper.setContextMenu(x, y, context);
        context.open();
    }

    @Override
    public GUIListener scroll(double x, double y) {
        if (UIHelper.isWidgetHovered(this)) {
            int i = selected;
            i += (int) Math.signum(-y);
            select((int) Maths.modulo(i, indexes.size()));
            return this;
        }

        return super.scroll(x, y);
    }
}
