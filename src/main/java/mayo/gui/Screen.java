package mayo.gui;

import mayo.Client;
import mayo.model.GeometryHelper;
import mayo.model.Renderable;
import mayo.render.BatchRenderer;
import mayo.render.MatrixStack;
import mayo.render.Shaders;
import mayo.render.Texture;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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

    public void render(BatchRenderer renderer, MatrixStack stack) {
        for (Widget widget : this.widgets) {
            widget.render(renderer, stack);
        }
    }

    public static void temp(BatchRenderer renderer) {
        int len = 100;
        int size = 16;
        int offset = 2;
        Texture texture = new Texture(Client.NAMESPACE, "blocks_new");

        Random r = new Random(42L);
        for (int i = 0; i < len; i++) {
            for (int j = 0; j < len; j++) {
                Renderable renderable = new Renderable(GeometryHelper.rectangle(i * (size + offset), j * (size + offset), size, size, texture, r.nextInt(5), r.nextInt(2), 5, 2, 1, 1));
                renderer.addElement(Shaders.MAIN, renderable);
            }
        }
    }

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
