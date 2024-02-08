package mayo.gui.widgets;

import mayo.Client;
import mayo.gui.widgets.types.ContextMenu;
import mayo.render.MatrixStack;
import mayo.render.Window;
import mayo.text.Text;
import mayo.utils.UIHelper;

import static org.lwjgl.glfw.GLFW.*;

public abstract class SelectableWidget extends Widget implements GUIListener {

    private boolean
            active = true,
            hovered = false,
            focused = false,
            selectable = true;
    private Text tooltip;
    private ContextMenu contextMenu;

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
        return (hovered && (this instanceof ContextMenu.ContextButton || !UIHelper.isMouseOverContext())) || (contextMenu != null && contextMenu.isOpen());
    }

    public boolean isHoveredOrFocused() {
        return isHovered() || isFocused();
    }

    protected void updateHover(int x, int y) {
        this.hovered = UIHelper.isMouseOver(this, x, y);
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
        if (contextMenu != null && !focused)
            contextMenu.close();
    }

    @Override
    public GUIListener mousePress(int button, int action, int mods) {
        if (isActive() && isHovered() && action == GLFW_PRESS && button == GLFW_MOUSE_BUTTON_2 && contextMenu != null) {
            Window w = Client.getInstance().window;
            openContext(w.mouseX, w.mouseY);
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
        if (isFocused() && action == GLFW_PRESS && key == GLFW_KEY_MENU && contextMenu != null) {
            openContext(getCenterX(), getCenterY());
            return this;
        }

        return GUIListener.super.keyPress(key, scancode, action, mods);
    }

    protected void openContext(int x, int y) {
        UIHelper.setContextMenu(x, y, contextMenu);
        contextMenu.open();
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        if (this.isHoveredOrFocused() && tooltip != null && (contextMenu == null || !contextMenu.isOpen()))
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

    public ContextMenu getContextMenu() {
        return contextMenu;
    }

    public void setContextMenu(ContextMenu contextMenu) {
        this.contextMenu = contextMenu;
        contextMenu.setParent(this);
    }

    public boolean isSelectable() {
        return selectable;
    }

    public void setSelectable(boolean selectable) {
        this.selectable = selectable;
    }
}
