package mayo.gui;

import mayo.Client;
import mayo.gui.widgets.Container;
import mayo.gui.widgets.Widget;
import mayo.gui.widgets.types.ContextMenu;
import mayo.model.GeometryHelper;
import mayo.model.Vertex;
import mayo.render.Font;
import mayo.render.MatrixStack;
import mayo.render.batch.VertexConsumer;
import mayo.text.Text;
import mayo.utils.UIHelper;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;

public abstract class Screen {

    //main container
    protected final Container mainContainer = new Container(0, 0);

    //screen-wise fields
    protected Client client;
    protected Font font;
    protected int width, height;

    //overlays
    public Text tooltip;
    public ContextMenu contextMenu;


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
        UIHelper.renderBackground(matrices, width, height, delta);
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
        //render tooltip
        if (tooltip != null) {
            if (!tooltip.isEmpty())
                UIHelper.renderTooltip(matrices, tooltip, client.font, mouseX, mouseY);
            tooltip = null;
        }
    }


    // -- listeners -- //


    public boolean mousePress(int button, int action, int mods) {
        return this.mainContainer.mousePress(button, action, mods);
    }

    public boolean keyPress(int key, int scancode, int action, int mods) {
        boolean child = this.mainContainer.keyPress(key, scancode, action, mods);
        if (child) return true;

        if (action == GLFW_PRESS && key == GLFW_KEY_ESCAPE && closeOnEsc())
            this.close();

        return false;
    }

    public boolean charTyped(char c, int mods) {
        return this.mainContainer.charTyped(c, mods);
    }

    public boolean mouseMove(int x, int y) {
        return this.mainContainer.mouseMove(x, y);
    }

    public boolean scroll(double x, double y) {
        return this.mainContainer.scroll(x, y);
    }

    public boolean windowFocused(boolean focused) {
        return this.mainContainer.windowFocused(focused);
    }
}
