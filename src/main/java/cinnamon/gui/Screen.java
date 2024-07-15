package cinnamon.gui;

import cinnamon.Client;
import cinnamon.gui.widgets.*;
import cinnamon.model.GeometryHelper;
import cinnamon.model.Vertex;
import cinnamon.render.Font;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.utils.Resource;
import cinnamon.utils.UIHelper;

import static org.lwjgl.glfw.GLFW.*;

public abstract class Screen {

    protected static final Resource[] BACKGROUND = new Resource[]{
            new Resource("textures/gui/background/background_0.png"),
            new Resource("textures/gui/background/background_1.png"),
            new Resource("textures/gui/background/background_2.png")
    };

    //main container
    protected final Container mainContainer = new Container(0, 0);
    protected SelectableWidget focused;

    //screen-wise fields
    protected Client client;
    protected Font font;
    protected int width, height;

    //overlays
    public SelectableWidget tooltip;
    public PopupWidget popup;


    // -- screen functions -- //


    //init from client
    public final void init(Client client, int width, int height) {
        this.client = client;
        this.font = client.font;
        this.width = width;
        this.height = height;
        this.rebuild();
    }

    //children-based init, also happens when the window is modified
    public void init() {}

    //screen is added into the client, ie one-time init
    public void added() {}

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
        this.mainContainer.clear();
        this.tooltip = null;
        if (this.popup != null)
            this.popup.close();
        this.popup = null;
        this.init();
    }

    public void resize(int width, int height) {
        this.width = width;
        this.height = height;
        this.rebuild();
    }

    public void addWidget(Widget widget) {
        this.mainContainer.addWidget(widget);
    }

    public void addWidgetOnTop(Widget widget) {
        this.mainContainer.addWidgetOnTop(widget);
    }

    public void removeWidget(Widget widget) {
        this.mainContainer.removeWidget(widget);
        if (widget == focused) focusWidget(null);
    }

    public void focusWidget(SelectableWidget widget) {
        if (this.focused == widget)
            return;

        if (this.focused != null)
            this.focused.setFocused(false);

        this.focused = widget;
        if (widget != null)
            widget.setFocused(true);
    }

    public SelectableWidget getFocusedWidget() {
        return focused;
    }

    public GUIListener getWidgetAt(int x, int y) {
        return mainContainer.getWidgetAt(x, y);
    }

    // -- tick -- //


    public void tick() {
        this.mainContainer.tick();
    }


    // -- render -- //


    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        preRender(matrices, mouseX, mouseY, delta);
        renderChildren(matrices, mouseX, mouseY, delta);
        postRender(matrices, mouseX, mouseY, delta);
    }

    protected void renderBackground(MatrixStack matrices, float delta) {
        UIHelper.renderBackground(matrices, width, height, delta, BACKGROUND);
    }

    protected void renderTranslucentBackground(MatrixStack matrices, float delta) {
        matrices.push();
        matrices.translate(0f, 0f, -999f);

        Vertex[] vertices = GeometryHelper.quad(matrices, 0, 0, width, height);
        for (Vertex vertex : vertices)
            vertex.color(0x88 << 24);
        VertexConsumer.GUI.consume(vertices, -1);

        matrices.pop();
    }

    protected void preRender(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices, delta);
    }

    protected void renderChildren(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.mainContainer.render(matrices, mouseX, mouseY, delta);
    }

    protected void postRender(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        if (tooltip != null) {
            UIHelper.renderTooltip(matrices, tooltip, client.font);
            tooltip = null;
        }
    }


    // -- listeners -- //


    public boolean mousePress(int button, int action, int mods) {
        GUIListener click = this.mainContainer.mousePress(button, action, mods);
        if (click != focused && action == GLFW_PRESS)
            focusWidget(null);
        return click != null;
    }

    public boolean keyPress(int key, int scancode, int action, int mods) {
        if (this.mainContainer.keyPress(key, scancode, action, mods) != null)
            return true;

        if (action != GLFW_PRESS)
            return false;

        switch (key) {
            case GLFW_KEY_ESCAPE -> {if (closeOnEsc()) this.close();}
            case GLFW_KEY_TAB -> focusWidget(mainContainer.selectNext(focused, (mods & GLFW_MOD_SHIFT) != 0));
            case GLFW_KEY_UP -> focusWidget(mainContainer.selectNext(focused, true));
            case GLFW_KEY_DOWN -> focusWidget(mainContainer.selectNext(focused, false));
            default -> {return false;}
        }

        return true;
    }

    public boolean charTyped(char c, int mods) {
        return this.mainContainer.charTyped(c, mods) != null;
    }

    public boolean mouseMove(int x, int y) {
        return this.mainContainer.mouseMove(x, y) != null;
    }

    public boolean scroll(double x, double y) {
        return this.mainContainer.scroll(x, y) != null;
    }

    public boolean windowFocused(boolean focused) {
        return this.mainContainer.windowFocused(focused) != null;
    }
}
