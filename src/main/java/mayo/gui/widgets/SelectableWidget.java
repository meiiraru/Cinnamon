package mayo.gui.widgets;

import mayo.Client;
import mayo.gui.widgets.types.ContextMenu;
import mayo.render.MatrixStack;
import mayo.render.Window;
import mayo.text.Text;
import mayo.utils.UIHelper;

public abstract class SelectableWidget extends Widget implements GUIListener {

    private boolean
            active = true,
            hovered = false,
            focused = false,
            selectable = true;
    private Text tooltip;

    public SelectableWidget(int x, int y, int width, int height) {
        super(x, y, width, height);
        Window w = Client.getInstance().window;
        updateHover(w.mouseX, w.mouseY);
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isHovered() {
        return hovered && (this instanceof ContextMenu.ContextButton || !UIHelper.hasActiveContextMenu());
    }

    public boolean isHoveredOrFocused() {
        return isHovered() || isFocused();
    }

    protected void updateHover(int x, int y) {
        this.hovered = UIHelper.isMouseOver(this, x, y);
    }

    public void setFocused(boolean focused) {
        this.focused = focused;
    }

    public boolean isFocused() {
        return focused;
    }

    @Override
    public GUIListener mousePress(int button, int action, int mods) {
        return isActive() && isHovered() ? this : GUIListener.super.mousePress(button, action, mods);
    }

    @Override
    public GUIListener mouseMove(int x, int y) {
        updateHover(x, y);
        return GUIListener.super.mouseMove(x, y);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        if (this.isHoveredOrFocused() && tooltip != null)
            UIHelper.setTooltip(this);

        renderWidget(matrices, mouseX, mouseY, delta);
    }

    public abstract void renderWidget(MatrixStack matrices, int mouseX, int mouseY, float delta);

    public Text getTooltip() {
        return tooltip;
    }

    public void setTooltip(Text tooltip) {
        this.tooltip = tooltip;
    }

    public boolean isSelectable() {
        return selectable;
    }

    public void setSelectable(boolean selectable) {
        this.selectable = selectable;
    }
}
