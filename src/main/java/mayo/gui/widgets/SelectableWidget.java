package mayo.gui.widgets;

import mayo.Client;
import mayo.render.MatrixStack;
import mayo.render.Window;
import mayo.text.Text;
import mayo.utils.UIHelper;

public abstract class SelectableWidget extends Widget implements GUIListener {

    private boolean
            active = true,
            hovered = false;
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
        return hovered;
    }

    public void setHovered(boolean hovered) {
        this.hovered = hovered;
    }

    protected void updateHover(int x, int y) {
        this.hovered = UIHelper.isMouseOver(this, x, y);
    }

    @Override
    public boolean mouseMove(int x, int y) {
        updateHover(x, y);
        return GUIListener.super.mouseMove(x, y);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        if (this.isHovered() && tooltip != null)
            UIHelper.setTooltip(tooltip);

        renderWidget(matrices, mouseX, mouseY, delta);
    }

    public abstract void renderWidget(MatrixStack matrices, int mouseX, int mouseY, float delta);

    public Text getTooltip() {
        return tooltip;
    }

    public void setTooltip(Text tooltip) {
        this.tooltip = tooltip;
    }
}
