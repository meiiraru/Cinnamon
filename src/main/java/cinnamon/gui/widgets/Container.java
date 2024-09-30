package cinnamon.gui.widgets;

import cinnamon.render.MatrixStack;
import cinnamon.utils.Maths;
import cinnamon.utils.UIHelper;

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
        if (widget instanceof Container c)
            c.updateDimensions();
        updateDimensions();
    }

    public void addWidgetOnTop(Widget widget) {
        this.widgets.addFirst(widget);
        if (widget instanceof GUIListener el)
            this.listeners.addFirst(el);
        if (widget instanceof Container c)
            c.updateDimensions();
        updateDimensions();
    }

    public void insertWidget(Widget widget, Widget position) {
        int index = this.widgets.indexOf(position);
        this.widgets.add(index + 1, widget);
        if (widget instanceof GUIListener el)
            this.listeners.add(index, el);
        if (widget instanceof Container c)
            c.updateDimensions();
        updateDimensions();
    }

    public void removeWidget(Widget widget) {
        this.widgets.remove(widget);
        if (widget instanceof GUIListener el)
            this.listeners.remove(el);
        updateDimensions();
    }

    public GUIListener getWidgetAt(int x, int y) {
        for (GUIListener listener : this.listeners) {
            if (UIHelper.isMouseOver((Widget) listener, x, y))
                return listener;
        }
        return null;
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

        this.updateDimensions(maxX - minX, maxY - minY);
    }

    protected void updateDimensions(int width, int height) {
        this.setDimensions(width, height);
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
    public GUIListener mousePress(int button, int action, int mods) {
        for (GUIListener listener : this.listeners) {
            GUIListener result = listener.mousePress(button, action, mods);
            if (result != null)
                return result;
        }
        return null;
    }

    @Override
    public GUIListener keyPress(int key, int scancode, int action, int mods) {
        for (GUIListener listener : this.listeners) {
            GUIListener result =  listener.keyPress(key, scancode, action, mods);
            if (result != null)
                return result;
        }
        return null;
    }

    @Override
    public GUIListener charTyped(char c, int mods) {
        for (GUIListener listener : this.listeners) {
            GUIListener result = listener.charTyped(c, mods);
            if (result != null)
                return result;
        }
        return null;
    }

    @Override
    public GUIListener mouseMove(int x, int y) {
        for (GUIListener listener : this.listeners) {
            GUIListener result = listener.mouseMove(x, y);
            if (result != null)
                return result;
        }
        return null;
    }

    @Override
    public GUIListener scroll(double x, double y) {
        for (GUIListener listener : this.listeners) {
            GUIListener result = listener.scroll(x, y);
            if (result != null)
                return result;
        }
        return null;
    }

    @Override
    public GUIListener windowFocused(boolean focused) {
        for (GUIListener listener : this.listeners) {
            GUIListener result = listener.windowFocused(focused);
            if (result != null)
                return result;
        }
        return null;
    }

    public SelectableWidget selectNext(SelectableWidget current, boolean backwards) {
        List<SelectableWidget> list = getSelectableWidgets();

        //no widget found, return null
        if (list.isEmpty())
            return null;

        //no widget is selected yet, return first
        if (current == null)
            return list.getFirst();

        //did not find selected widget, return first
        int i = list.indexOf(current);
        if (i == -1)
            return list.getFirst();

        //change selected index
        i = backwards ? i - 1 : i + 1;
        i = Maths.modulo(i, list.size());

        //return new selected widget
        return list.get(i);
    }

    protected List<SelectableWidget> getSelectableWidgets() {
        List<SelectableWidget> list = new ArrayList<>();

        for (Widget widget : widgets) {
            if (widget instanceof SelectableWidget w && w.isSelectable())
                list.add(w);
            else if (widget instanceof Container c)
                list.addAll(c.getSelectableWidgets());
        }

        return list;
    }
}
