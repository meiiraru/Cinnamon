package mayo.gui.widgets.types;

import mayo.Client;
import mayo.gui.widgets.Widget;
import mayo.render.Font;
import mayo.render.MatrixStack;
import mayo.render.Texture;
import mayo.render.Window;
import mayo.render.batch.VertexConsumer;
import mayo.text.Text;
import mayo.utils.Resource;
import mayo.utils.TextUtils;
import mayo.utils.UIHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;

public class ContextMenu extends WidgetList {

    private static final Texture TEXTURE = Texture.of(new Resource("textures/gui/widgets/context_menu.png"));

    private final List<ContextButton> actions = new ArrayList<>();
    private final Widget parent;
    private final int minWidth;
    private final int elementHeight;
    private boolean open;

    public ContextMenu(int minWidth, int elementHeight, Widget owner) {
        super(0, 0, 0);
        this.minWidth = minWidth;
        this.elementHeight = elementHeight;
        this.parent = owner;
        this.setDimensions(minWidth, 6);
    }

    public boolean isOpen() {
        return open;
    }

    public void close() {
        this.open = false;
    }

    public void open() {
        this.open = true;
    }

    public void addAction(Text name, Text tooltip, Consumer<Button> action) {
        int y = widgets.isEmpty() ? 0 : getHeight();
        int width = Math.max(TextUtils.getWidth(name, Client.getInstance().font) + 4, minWidth - 2);

        ContextButton button = new ContextButton(getX(), y, name, action, widgets.size());
        button.setTooltip(tooltip);
        button.setDimensions(width, elementHeight);

        addAction(button);
    }

    private void addAction(ContextButton button) {
        this.addWidget(button);
        this.actions.add(button);
    }

    public Button getAction(int i) {
        return actions.get(i);
    }

    @Override
    public void updateDimensions() {
        super.updateDimensions();
        int w = this.getWidth();
        for (Widget widget : widgets)
            widget.setWidth(w);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        if (!isOpen())
            return;

        matrices.push();
        matrices.translate(0f, 0f, 500f);

        //render background
        renderBackground(matrices, mouseX, mouseY, delta);

        //render child
        super.render(matrices, mouseX, mouseY, delta);

        matrices.pop();
    }

    protected void renderBackground(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        UIHelper.nineQuad(
                VertexConsumer.GUI, matrices, TEXTURE.getID(),
                getX() - 1, getY() - 1,
                getWidth() + 2, getHeight() + 2,
                0f, 0f,
                16, 16,
                32, 32
        );
    }

    @Override
    public boolean mousePress(int button, int action, int mods) {
        if (!isOpen())
            return false;

        Window w = Client.getInstance().window;
        if (action == GLFW_PRESS) {
            if (UIHelper.isMouseOver(parent, w.mouseX, w.mouseY))
                return false; //void this when clicking on the parent widget

            if (!UIHelper.isMouseOver(this, w.mouseX, w.mouseY)) {
                this.close();
                return false; //do not void mouse click, however do not allow for children click
            }
        }

        super.mousePress(button, action, mods);
        return true; //always void mouse click for other widgets
    }

    @Override
    public boolean keyPress(int key, int scancode, int action, int mods) {
        if (!isOpen())
            return false;

        if (action == GLFW_PRESS && key == GLFW_KEY_ESCAPE) {
            this.close();
            return true;
        }

        return super.keyPress(key, scancode, action, mods);
    }

    public static class ContextButton extends Button {
        private final int index;

        public ContextButton(int x, int y, Text message, Consumer<Button> action, int index) {
            super(x, y, 0, 0, message, action);
            this.index = index;
        }

        @Override
        protected void renderBackground(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            UIHelper.nineQuad(
                    VertexConsumer.GUI, matrices, TEXTURE.getID(),
                    getX(), getY(),
                    getWidth(), getHeight(),
                    isHovered() ? 16f : (index % 2) * 16f, isHovered() ? 0f : 16f,
                    16, 16,
                    32, 32
            );
        }

        @Override
        protected void renderText(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            Font f = Client.getInstance().font;
            int x = getX() + 2;
            int y = getCenterY() - TextUtils.getHeight(message, f) / 2;
            f.render(VertexConsumer.FONT, matrices, x, y, message);
        }
    }
}
