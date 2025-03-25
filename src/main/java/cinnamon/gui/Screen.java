package cinnamon.gui;

import cinnamon.Client;
import cinnamon.gui.widgets.Container;
import cinnamon.gui.widgets.GUIListener;
import cinnamon.gui.widgets.PopupWidget;
import cinnamon.gui.widgets.SelectableWidget;
import cinnamon.gui.widgets.Widget;
import cinnamon.model.GeometryHelper;
import cinnamon.model.SimpleGeometry;
import cinnamon.model.Vertex;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.render.shader.Shader;
import cinnamon.render.shader.Shaders;
import cinnamon.utils.Resource;
import cinnamon.utils.UIHelper;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.glDepthMask;

public abstract class Screen {

    protected static final Resource[] BACKGROUND = new Resource[]{
            new Resource("textures/gui/background/background_0.png"),
            new Resource("textures/gui/background/background_1.png"),
            new Resource("textures/gui/background/background_2.png"),
            new Resource("textures/gui/background/background_3.png")
    };

    //main container
    protected final Container mainContainer = new Container(0, 0);
    protected SelectableWidget focused;

    //screen-wise fields
    protected Client client;
    protected int width, height;

    //overlays
    public SelectableWidget tooltip;
    public PopupWidget popup;


    // -- screen functions -- //


    //init from client
    public final void init(Client client, int width, int height) {
        this.client = client;
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

        SelectableWidget prevFocus = this.focused;
        this.focused = widget;
        if (widget != null)
            widget.setFocused(true);

        if (prevFocus != null)
            prevFocus.setFocused(false);
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
        //UIHelper.renderBackground(matrices, width, height, delta, BACKGROUND);

        /*
        int c1 = 0xFF9875C9;
        int c2 = 0xFFFEE9CC;
        int c3 = 0xFF8CD7F7;
        int c4 = 0xFF6D5DDA;

        float t = (float) Math.sin((client.ticks + delta) * 0.01f) * 0.5f + 0.5f;
        int a = ColorUtils.lerpARGBColor(c1, c3, t);
        int b = ColorUtils.lerpARGBColor(c2, c4, t);
        int c = ColorUtils.lerpARGBColor(c3, c2, t);
        int d = ColorUtils.lerpARGBColor(c4, c1, t);

        GeometryHelper.drawRectangle(VertexConsumer.GUI, matrices, 0, 0, width, height, -999, a, b, c, d);
         */

        Shader oldShader = Shader.activeShader;
        Shader s = Shaders.MAIN_MENU.getShader().use();
        float time = (client.ticks + delta) * 0.05f;
        s.setFloat("time", time);
        s.setFloat("scale", (float) Math.sin(time * 0.5f) * 0.5f + 1f);
        s.setColor("color1", 0x8163AB);
        s.setColor("color2", 0xD8C6AD);
        s.setColor("color3", 0x77B7D2);
        s.setColor("color4", 0x5D4FB9);

        glDepthMask(false);
        SimpleGeometry.QUAD.render();
        glDepthMask(true);

        if (oldShader != null)
            oldShader.use();
    }

    protected void renderTranslucentBackground(MatrixStack matrices, float delta) {
        matrices.push();
        matrices.translate(0f, 0f, -UIHelper.getDepthOffset());

        Vertex[] vertices = GeometryHelper.quad(matrices, 0, 0, width, height);
        for (Vertex vertex : vertices)
            vertex.color(0x88 << 24);
        VertexConsumer.GUI.consume(vertices);

        matrices.pop();
    }

    protected void preRender(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices, delta);
    }

    protected void renderChildren(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.mainContainer.render(matrices, mouseX, mouseY, delta);
    }

    protected void postRender(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        boolean hasTooltip = tooltip != null;
        float d = UIHelper.getDepthOffset();

        if (hasTooltip) {
            matrices.push();
            matrices.translate(0, 0, 5f);
            tooltip.renderTooltip(matrices);
            tooltip = null;
            matrices.pop();
        }

        if (!client.camera.isOrtho()) {
            matrices.push();
            matrices.translate(0f, 0f, 3f);
            VertexConsumer.GUI.consume(GeometryHelper.quad(matrices, mouseX - 16, mouseY - 16, 32, 32), GUIStyle.getDefault().cursor);
            matrices.pop();
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

    public boolean filesDropped(String[] files) {
        return this.mainContainer.filesDropped(files) != null;
    }
}
