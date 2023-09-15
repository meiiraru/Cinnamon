package mayo.gui;

import mayo.Client;
import mayo.gui.widgets.GUIListener;
import mayo.gui.widgets.Tickable;
import mayo.gui.widgets.Widget;
import mayo.render.Font;
import mayo.render.MatrixStack;
import mayo.utils.UIHelper;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;

public abstract class Screen {

    //widget lists
    private final List<Widget> widgets = new ArrayList<>();
    private final List<GUIListener> listeners = new ArrayList<>();

    //screen-wise fields
    protected Client client;
    protected Font font;
    protected int width, height;


    // -- screen functions -- //


    //init from client
    public final void init(Client client, int width, int height) {
        this.client = client;
        this.font = client.font;
        this.width = width;
        this.height = height;
        this.rebuild();
    }

    //children-based init
    public void init() {}

    //screen is removed from client, ie saving changes
    public void removed() {}

    //if screen should be closed from GLFW_KEY_ESCAPE PRESS
    public boolean closeOnEsc() {
        return false;
    }

    //when closing the screen (not removing), call this
    public void close() {
        this.client.setScreen(null);
    }


    // -- widgets management -- //


    public void rebuild() {
        this.widgets.clear();
        this.listeners.clear();
        this.init();
    }

    public void resize(int width, int height) {
        this.width = width;
        this.height = height;
        this.rebuild();
    }

    public void addWidget(Widget widget) {
        this.widgets.add(widget);
        if (widget instanceof GUIListener el)
            this.listeners.add(el);
    }

    public void removeWidget(Widget widget) {
        this.widgets.remove(widget);
        if (widget instanceof GUIListener el)
            this.listeners.remove(el);
    }


    // -- tick -- //


    public void tick() {
        for (Widget widget : this.widgets) {
            if (widget instanceof Tickable t)
                t.tick();
        }
    }


    // -- render -- //


    public void render(MatrixStack matrices, float delta) {
        preRender(matrices, delta);
        renderChildren(matrices, client.mouseX, client.mouseY, delta);
        postRender(matrices, delta);
    }

    protected void renderBackground(MatrixStack matrices, float delta) {
        UIHelper.renderBackground(matrices, delta);
    }

    protected void preRender(MatrixStack matrices, float delta) {
        this.renderBackground(matrices, delta);
    }

    protected void renderChildren(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        for (Widget widget : this.widgets) {
            widget.render(matrices, mouseX, mouseY, delta);
        }
    }

    protected void postRender(MatrixStack matrices, float delta) {}


    // -- listeners -- //


    public void mousePress(int button, int action, int mods) {
        for (GUIListener listener : this.listeners) {
            if (listener.mousePress(button, action, mods))
                break;
        }
    }

    public void keyPress(int key, int scancode, int action, int mods) {
        for (GUIListener listener : this.listeners) {
            if (listener.keyPress(key, scancode, action, mods))
                break;
        }

        if (action == GLFW_PRESS && key == GLFW_KEY_ESCAPE && closeOnEsc())
            this.close();
    }

    public void charTyped(char c, int mods) {
        for (GUIListener listener : this.listeners) {
            if (listener.charTyped(c, mods))
                break;
        }
    }

    public void mouseMove(double x, double y) {
        for (GUIListener listener : this.listeners) {
            if (listener.mouseMove(x, y))
                break;
        }
    }

    public void scroll(double x, double y) {
        for (GUIListener listener : this.listeners) {
            if (listener.scroll(x, y))
                break;
        }
    }

    public void windowFocused(boolean focused) {
        for (GUIListener listener : this.listeners) {
            if (listener.windowFocused(focused))
                break;
        }
    }
}
