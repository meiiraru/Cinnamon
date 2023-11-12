package mayo.gui.widgets;

import mayo.Client;
import mayo.render.Window;
import mayo.utils.UIHelper;

public abstract class SelectableWidget extends Widget implements GUIListener {

    private boolean
            active = true,
            hovered = false;

    public SelectableWidget(int x, int y, int width, int height) {
        super(x, y, width, height);
        Window w = Client.getInstance().window;
        updateHover(w.mouseX, w.mouseY);
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isHovered() {
        return hovered;
    }

    private void updateHover(int x, int y) {
        this.hovered = UIHelper.isMouseOver(this, x, y);
    }

    @Override
    public boolean mouseMove(int x, int y) {
        updateHover(x, y);
        return GUIListener.super.mouseMove(x, y);
    }
}
