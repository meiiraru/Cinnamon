package mayo.gui;

import mayo.gui.widgets.Tickable;
import mayo.gui.widgets.Widget;
import mayo.render.MatrixStack;
import mayo.utils.UIHelper;

import java.util.ArrayList;
import java.util.List;

public class Screen {

    protected final List<Widget> widgets = new ArrayList<>();

    public void addWidget(Widget widget) {
        this.widgets.add(widget);
    }

    public void removeWidget(Widget widget) {
        this.widgets.remove(widget);
    }

    public void tick() {
        for (Widget widget : this.widgets) {
            if (widget instanceof Tickable t)
                t.tick();
        }
    }

    public void render(MatrixStack matrices, float delta) {
        preRender(matrices, delta);
        renderChildren(matrices, delta);
        postRender(matrices, delta);
    }

    protected void preRender(MatrixStack matrices, float delta) {
        UIHelper.renderBackground(matrices, delta);
    }

    protected void renderChildren(MatrixStack matrices, float delta) {
        for (Widget widget : this.widgets) {
            widget.render(matrices);
        }
    }

    protected void postRender(MatrixStack matrices, float delta) {}

    public void mousePress(int button, int action, int mods) {
        for (Widget widget : this.widgets) {
            if (widget.mousePress(button, action, mods))
                break;
        }
    }

    public void keyPress(int key, int scancode, int action, int mods) {
        for (Widget widget : this.widgets) {
            if (widget.keyPress(key, scancode, action, mods))
                break;
        }
    }

    public void charTyped(char c, int mods) {
        for (Widget widget : this.widgets) {
            if (widget.charTyped(c, mods))
                break;
        }
    }

    public void mouseMove(double x, double y) {
        for (Widget widget : this.widgets) {
            if (widget.mouseMove(x, y))
                break;
        }
    }

    public void scroll(double x, double y) {
        for (Widget widget : this.widgets) {
            if (widget.scroll(x, y))
                break;
        }
    }

    public void windowResize(int width, int height) {
        for (Widget widget : this.widgets) {
            if (widget.windowResize(width, height))
                break;
        }
    }

    public void windowFocused(boolean focused) {
        for (Widget widget : this.widgets) {
            if (widget.windowFocused(focused))
                break;
        }
    }
}
