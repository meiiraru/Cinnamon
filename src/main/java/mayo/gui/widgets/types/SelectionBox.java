package mayo.gui.widgets.types;

import mayo.Client;
import mayo.gui.widgets.Container;
import mayo.render.Font;
import mayo.render.MatrixStack;
import mayo.render.Window;
import mayo.render.batch.VertexConsumer;
import mayo.text.Style;
import mayo.text.Text;
import mayo.utils.Maths;
import mayo.utils.TextUtils;
import mayo.utils.UIHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class SelectionBox extends Container {

    private final List<Text> indexes = new ArrayList<>();
    protected ContextMenu context;

    protected Text selectedText = Text.of("-");
    protected int selected = -1;
    protected Consumer<Integer> changeListener;

    protected boolean closeOnSelect;

    public SelectionBox(int x, int y, int width, int height) {
        super(x, y);
        addWidget(new Button(x, y, width, height, Text.of(""), button -> {
            if (context.isOpen()) {
                context.close();
            } else {
                UIHelper.setContextMenu(getX(), getY() + getHeight(), context);
                context.open();
            }
        }) {
            @Override
            protected void renderText(MatrixStack matrices, int mouseX, int mouseY, float delta) {
                Font f = Client.getInstance().font;

                //render arrow
                Text text = Text.of(isExpanded() ? "\u23F6" : "\u23F7");
                int width = TextUtils.getWidth(text, f);
                int x = getX() + getWidth() - width - 2;
                int y = getCenterY() - TextUtils.getHeight(text, f) / 2;
                f.render(VertexConsumer.FONT_FLAT, matrices, x, y, text);

                //render selected text
                text = TextUtils.addEllipsis(selectedText, f, getWidth() - width - 4);
                x = getX() + 2;
                y = getCenterY() - TextUtils.getHeight(text, f) / 2;
                f.render(VertexConsumer.FONT_FLAT, matrices, x, y, text);
            }

            @Override
            public boolean isHovered() {
                return context.isOpen() || super.isHovered();
            }
        });

        context = new ContextMenu(width, height, this);
    }

    public boolean isExpanded() {
        return context.isOpen();
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
            context.close();
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

        context.addAction(name, tooltip, button -> {
            select(index);
            if (action != null)
                action.accept(button);
        });

        return this;
    }

    private void updateTexts() {
        for (int i = 0; i < indexes.size(); i++) {
            //get text
            Text text = indexes.get(i);

            //selected entry
            if (i == selected)
                text = Text.empty().withStyle(Style.EMPTY.color(UIHelper.ACCENT)).append(text);

            //apply text
            context.getAction(i).setMessage(text);
        }
    }

    @Override
    public boolean scroll(double x, double y) {
        Window w = Client.getInstance().window;
        if (UIHelper.isMouseOver(this, w.mouseX, w.mouseY)) {
            int i = selected;
            i += (int) Math.signum(-y);
            select((int) Maths.modulo(i, indexes.size()));
            return true;
        }

        return super.scroll(x, y);
    }
}
