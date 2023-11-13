package mayo.gui.widgets;

import mayo.render.MatrixStack;

import java.util.ArrayList;
import java.util.List;

public class Container extends Widget implements Tickable, GUIListener {

    protected final List<Widget> widgets = new ArrayList<>();
    protected final List<GUIListener> listeners = new ArrayList<>();

    public Container(int x, int y) {
        super(x, y, 0, 0);
    }

    public void addWidget(Widget widget) {
        this.widgets.add(widget);
        if (widget instanceof GUIListener el)
            this.listeners.add(el);
        updateDimensions();
    }

    public void addWidgetOnTop(Widget widget) {
        this.widgets.add(0, widget);
        if (widget instanceof GUIListener el)
            this.listeners.add(0, el);
        updateDimensions();
    }

    public void removeWidget(Widget widget) {
        this.widgets.remove(widget);
        if (widget instanceof GUIListener el)
            this.listeners.remove(el);
        updateDimensions();
    }

    public void clear() {
        this.widgets.clear();
        this.listeners.clear();
        this.setDimensions(0, 0);
    }

    public void updateDimensions() {
        int minX = getX(), maxX = minX;
        int minY = getY(), maxY = minY;

        for (Widget w : this.widgets) {
            int x = w.getX();
            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x + w.getWidth());

            int y = w.getY();
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y + w.getHeight());
        }

        this.setDimensions(maxX - minX, maxY - minY);
    }

    @Override
    public void tick() {
        for (Widget widget : this.widgets) {
            if (widget instanceof Tickable t)
                t.tick();
        }
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        for (Widget widget : this.widgets)
            widget.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    public void setX(int x) {
        int d = x - getX();
        super.setX(x);

        for (Widget widget : this.widgets)
            widget.setX(widget.getX() + d);
    }

    @Override
    public void setY(int y) {
        int d = y - getY();
        super.setY(y);

        for (Widget widget : this.widgets)
            widget.setY(widget.getY() + d);
    }

    // -- INPUT LISTENER -- //

    @Override
    public boolean mousePress(int button, int action, int mods) {
        for (GUIListener listener : this.listeners) {
            if (listener.mousePress(button, action, mods))
                return true;
        }
        return false;
    }

    @Override
    public boolean keyPress(int key, int scancode, int action, int mods) {
        for (GUIListener listener : this.listeners) {
            if (listener.keyPress(key, scancode, action, mods))
                return true;
        }
        return false;
    }

    @Override
    public boolean charTyped(char c, int mods) {
        for (GUIListener listener : this.listeners) {
            if (listener.charTyped(c, mods))
                return true;
        }
        return false;
    }

    @Override
    public boolean mouseMove(int x, int y) {
        for (GUIListener listener : this.listeners) {
            if (listener.mouseMove(x, y))
                return true;
        }
        return false;
    }

    @Override
    public boolean scroll(double x, double y) {
        for (GUIListener listener : this.listeners) {
            if (listener.scroll(x, y))
                return true;
        }
        return false;
    }

    @Override
    public boolean windowFocused(boolean focused) {
        for (GUIListener listener : this.listeners) {
            if (listener.windowFocused(focused))
                return true;
        }
        return false;
    }
}
