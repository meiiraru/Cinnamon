package cinnamon.gui.widgets;

import cinnamon.Client;
import cinnamon.gui.widgets.types.ContextMenu;
import cinnamon.render.MatrixStack;
import cinnamon.render.Window;
import cinnamon.text.Text;
import cinnamon.utils.UIHelper;

import static org.lwjgl.glfw.GLFW.*;

public abstract class SelectableWidget extends Widget implements GUIListener {

    private boolean
            active = true,
            hovered = false,
            focused = false,
            selectable = true;
    private Text tooltip;
    private PopupWidget popup;

    public SelectableWidget(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isHovered() {
        return (hovered && (this instanceof ContextMenu.ContextButton || !UIHelper.isPopupHovered())) || (popup != null && popup.isOpen());
    }

    public boolean isHoveredOrFocused() {
        return isHovered() || isFocused();
    }

    protected void updateHover(int x, int y) {
        setHovered(UIHelper.isMouseOver(this, x, y));
    }

    protected void setHovered(boolean hovered) {
        this.hovered = hovered;
    }

    public void setFocused(boolean focused) {
        if (this.focused != focused)
            onFocusChange(focused);
        this.focused = focused;
    }

    public boolean isFocused() {
        return focused;
    }

    protected void onFocusChange(boolean focused) {
        if (popup != null && !focused)
            popup.close();
    }

    public int getState() {
        if (!this.isActive())
            return 0;
        else if (this.isHoveredOrFocused())
            return 2;
        else
            return 1;
    }

    @Override
    public GUIListener mousePress(int button, int action, int mods) {
        if (isActive() && isHovered() && action == GLFW_PRESS && button == GLFW_MOUSE_BUTTON_2 && popup != null) {
            Window w = Client.getInstance().window;
            openPopup(w.mouseX, w.mouseY);
            return this;
        }

        return GUIListener.super.mousePress(button, action, mods);
    }

    @Override
    public GUIListener mouseMove(int x, int y) {
        updateHover(x, y);
        return GUIListener.super.mouseMove(x, y);
    }

    @Override
    public GUIListener keyPress(int key, int scancode, int action, int mods) {
        if (isFocused() && action == GLFW_PRESS && key == GLFW_KEY_MENU && popup != null) {
            openPopup(getCenterX(), getCenterY());
            return this;
        }

        return GUIListener.super.keyPress(key, scancode, action, mods);
    }

    protected void openPopup(int x, int y) {
        UIHelper.setPopup(x, y, popup);
        popup.open();
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        if (this.isHoveredOrFocused() && tooltip != null && (popup == null || !popup.isOpen()))
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

    public PopupWidget getPopup() {
        return popup;
    }

    public void setPopup(PopupWidget popup) {
        this.popup = popup;
        popup.setParent(this);
    }

    public boolean isSelectable() {
        return selectable;
    }

    public void setSelectable(boolean selectable) {
        this.selectable = selectable;
    }
}
