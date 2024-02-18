package mayo.gui.widgets;

import mayo.render.MatrixStack;
import mayo.utils.UIHelper;

import java.util.List;

import static org.lwjgl.glfw.GLFW.*;

public class PopupWidget extends ContainerGrid {

    private boolean open;
    private Widget parent;
    private boolean hovered;

    public PopupWidget(int x, int y, int spacing) {
        super(x, y, spacing);
    }

    public boolean isOpen() {
        return open;
    }

    public boolean isHovered() {
        return hovered;
    }

    public void close() {
        this.open = false;
        reset();
    }

    public void open() {
        this.open = true;
        reset();
    }

    protected void reset() {}

    public Widget getParent() {
        return parent;
    }

    public void setParent(Widget parent) {
        this.parent = parent;
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        if (!isOpen())
            return;

        matrices.push();
        matrices.translate(0f, 0f, parent instanceof PopupWidget ? 1f : 500f);

        //render this
        renderWidget(matrices, mouseX, mouseY, delta);

        //render child
        super.render(matrices, mouseX, mouseY, delta);

        matrices.pop();
    }

    protected void renderWidget(MatrixStack matrices, int mouseX, int mouseY, float delta) {

    };

    @Override
    public GUIListener mousePress(int button, int action, int mods) {
        if (!isOpen())
            return null;

        //check if a child is being pressed first
        GUIListener sup = super.mousePress(button, action, mods);
        if (sup != null) return sup;

        //close popup when clicked outside it, but do not void the mouse click
        if (action == GLFW_PRESS && !UIHelper.isWidgetHovered(this)) {
            this.close();
            return null;
        }

        //always void mouse click when clicking somewhere inside it
        return this;
    }

    @Override
    public GUIListener mouseMove(int x, int y) {
        this.hovered = UIHelper.isMouseOver(this, x, y);
        return super.mouseMove(x, y);
    }

    @Override
    public GUIListener keyPress(int key, int scancode, int action, int mods) {
        if (!isOpen())
            return null;

        if (action != GLFW_PRESS)
            return super.keyPress(key, scancode, action, mods);

        switch (key) {
            case GLFW_KEY_ESCAPE -> this.close();
            case GLFW_KEY_DOWN -> {} //this.selectNext(false);
            case GLFW_KEY_UP -> {} //this.selectNext(true);
            default -> super.keyPress(key, scancode, action, mods);
        }

        return this;
    }

    @Override
    protected List<SelectableWidget> getSelectableWidgets() {
        return List.of();
    }
}
