package cinnamon.gui.widgets;

import cinnamon.Client;
import cinnamon.render.MatrixStack;
import cinnamon.render.Window;
import cinnamon.text.Style;
import cinnamon.text.Text;
import cinnamon.utils.Maths;
import cinnamon.utils.Resource;
import cinnamon.utils.TextUtils;
import cinnamon.utils.UIHelper;
import cinnamon.vr.XrManager;

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
        if (hovered && UIHelper.isPopupHovered()) {
            boolean isPopup = false;
            Widget parent = this;
            while ((parent = parent.getParent()) != null) {
                if (parent instanceof PopupWidget pw && pw.isHovered()) {
                    isPopup = true;
                    break;
                }
            }

            return isPopup;
        }

        return hovered;
    }

    public boolean isHoveredOrFocused() {
        return isHovered() || isFocused();
    }

    protected void updateHover(int x, int y) {
        setHovered(UIHelper.isMouseOver(this, x, y));
    }

    protected void setHovered(boolean hovered) {
        if (hovered && isActive() && XrManager.isInXR())
            UIHelper.xrWidgetHovered(this);
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
        if (!focused && popup != null && popup.isOpen() && !popup.isHovered())
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
        if (isActive() && isHovered() && button == GLFW_MOUSE_BUTTON_2 && popup != null) {
            if (action == GLFW_RELEASE) {
                Window w = Client.getInstance().window;
                openPopup(w.mouseX, w.mouseY);
            }
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
        popup.setStyle(getStyleRes());
    }

    public boolean isSelectable() {
        return selectable;
    }

    protected void setSelectable(boolean selectable) {
        this.selectable = selectable;
    }

    public void renderTooltip(MatrixStack matrices) {
        //grab text
        Text tooltip = getTooltip();
        if (tooltip == null)
            return;

        tooltip = Text.empty().withStyle(Style.EMPTY.guiStyle(getStyleRes())).append(tooltip);

        //dimensions
        int w = TextUtils.getWidth(tooltip);
        int h = TextUtils.getHeight(tooltip);

        int wx = this instanceof AlignedWidget aw ? aw.getAlignedX() : getX();
        int cx = getCenterX();
        int cy = getCenterY();

        Window window = Client.getInstance().window;
        int screenW = window.getGUIWidth();
        int screenH = window.getGUIHeight();

        int b = getStyle().getInt("tooltip_border");
        boolean left = false;

        int x = wx + getWidth() + b + 4;
        int y = cy - h / 2;

        //boundaries test
        if (x + w + b > screenW && cx > screenW / 2) {
            x = wx - w - b - 4;
            left = true;
        }
        x = Maths.clamp(x, b, screenW - w - b);
        y = Maths.clamp(y, b, screenH - h - b);

        //render
        UIHelper.renderTooltip(matrices, x, y, w, h, cx, cy, (byte) (left ? 1 : 0), tooltip, getStyle());
    }

    @Override
    public void setStyle(Resource style) {
        super.setStyle(style);
        if (this.popup != null)
            this.popup.setStyle(style);
    }
}
