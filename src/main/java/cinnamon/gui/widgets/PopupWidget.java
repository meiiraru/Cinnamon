package cinnamon.gui.widgets;

import cinnamon.Client;
import cinnamon.gui.Screen;
import cinnamon.gui.widgets.types.Scrollbar;
import cinnamon.render.MatrixStack;
import cinnamon.utils.UIHelper;
import org.joml.Math;

import java.util.List;
import java.util.function.Consumer;

import static org.lwjgl.glfw.GLFW.*;

public class PopupWidget extends ContainerGrid {

    private boolean open;
    private boolean hovered;
    private boolean closeOnSelect = true;
    private Consumer<PopupWidget> openListener;
    private Consumer<PopupWidget> closeListener;
    private boolean wasParentFocused;
    private boolean forceFocusParent;
    private boolean voidOutsideClicks = true;
    private boolean closeOnEscape = true;

    public PopupWidget(int x, int y, int spacing) {
        super(x, y, spacing);
    }

    public PopupWidget(int x, int y, int spacing, int columns) {
        super(x, y, spacing, columns);
    }

    public boolean isOpen() {
        return open;
    }

    public boolean isHovered() {
        return hovered;
    }

    public void close() {
        if (!isOpen())
            return;

        this.open = false;
        reset();

        if (closeListener != null)
            closeListener.accept(this);

        if ((forceFocusParent || wasParentFocused) && getParent() instanceof SelectableWidget sw)
            UIHelper.focusWidget(sw);
    }

    public void open() {
        if (isOpen())
            return;

        this.open = true;
        reset();

        wasParentFocused = getParent() instanceof SelectableWidget sw && sw.isFocused();

        if (openListener != null)
            openListener.accept(this);
    }

    protected void reset() {}

    public void setOpenListener(Consumer<PopupWidget> openListener) {
        this.openListener = openListener;
    }

    public void setCloseListener(Consumer<PopupWidget> closeListener) {
        this.closeListener = closeListener;
    }

    public void setForceFocusParent(boolean bool) {
        this.forceFocusParent = bool;
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        if (!isOpen())
            return;

        matrices.pushMatrix();
        float d = UIHelper.getDepthOffset();
        matrices.translate(0f, 0f, getParent() instanceof PopupWidget ? d * 2f : d * 5f);

        //render this
        renderWidget(matrices, mouseX, mouseY, delta);

        //render child
        matrices.translate(0f, 0f, d);
        super.render(matrices, mouseX, mouseY, delta);

        matrices.popMatrix();
    }

    protected void renderWidget(MatrixStack matrices, int mouseX, int mouseY, float delta) {

    }

    @Override
    public GUIListener mousePress(int button, int action, int mods) {
        if (!isOpen())
            return null;

        //check if a child is being pressed first
        GUIListener sup = super.mousePress(button, action, mods);
        if (sup != null) {
            if (!(sup instanceof Scrollbar) && action == GLFW_RELEASE && closeOnSelect && !(sup instanceof PopupWidget))
                this.close();
            return sup;
        }

        //close popup when clicked outside it, but do not void the mouse click
        if (voidOutsideClicks && action != GLFW_RELEASE && !this.isHovered()) {
            this.close();
            return null;
        }

        //and always void mouse click when clicking somewhere inside it
        return this;
    }

    @Override
    public GUIListener mouseMove(int x, int y) {
        if (!isOpen())
            return null;

        this.hovered = UIHelper.isMouseOver(this, x, y);
        return super.mouseMove(x, y);
    }

    @Override
    public GUIListener keyPress(int key, int scancode, int action, int mods) {
        if (!isOpen())
            return null;

        //children first
        GUIListener sup = super.keyPress(key, scancode, action, mods);
        if (sup != null)
            return sup;

        //only handle key presses
        if (action != GLFW_PRESS)
            return this;

        //handle navigation keys for this popup only
        Screen screen = Client.getInstance().screen;
        if (screen != null) {
            switch (key) {
                case GLFW_KEY_ESCAPE -> {if (closeOnEscape) this.close();}
                case GLFW_KEY_TAB  -> screen.focusWidget(this.selectNext(screen.getFocusedWidget(), (mods & GLFW_MOD_SHIFT) != 0, true));
                case GLFW_KEY_UP   -> screen.focusWidget(this.selectNext(screen.getFocusedWidget(), true, false));
                case GLFW_KEY_DOWN -> screen.focusWidget(this.selectNext(screen.getFocusedWidget(), false, false));
            }
        }

        //consume the event
        return this;
    }

    @Override
    public GUIListener scroll(double x, double y) {
        if (!isOpen())
            return null;

        //let children handle the scroll first
        GUIListener sup = super.scroll(x, y);
        if (sup != null)
            return sup;

        //if no child handled it, consume the scroll event to prevent it from affecting the main container
        return this;
    }

    @Override
    protected List<SelectableWidget> getSelectableWidgets(boolean isTab) {
        if (!isOpen()) return List.of();
        return super.getSelectableWidgets(isTab);
    }

    public PopupWidget closeOnSelect(boolean bool) {
        this.closeOnSelect = bool;
        return this;
    }

    public void fitToScreen(int width, int height) {
        this.setDimensions(Math.min(getWidth(), width), Math.min(getHeight(), height));
    }

    public void voidOutsideClicks(boolean voidOutsideClicks) {
        this.voidOutsideClicks = voidOutsideClicks;
    }

    public void closeOnEscape(boolean closeOnEscape) {
        this.closeOnEscape = closeOnEscape;
    }
}
