package mayo.gui.widgets.types;

import mayo.Client;
import mayo.gui.widgets.GUIListener;
import mayo.gui.widgets.SelectableWidget;
import mayo.gui.widgets.Widget;
import mayo.model.GeometryHelper;
import mayo.render.Font;
import mayo.render.MatrixStack;
import mayo.render.Texture;
import mayo.render.batch.VertexConsumer;
import mayo.text.Text;
import mayo.utils.Maths;
import mayo.utils.Resource;
import mayo.utils.TextUtils;
import mayo.utils.UIHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.lwjgl.glfw.GLFW.*;

public class ContextMenu extends WidgetList {

    private static final Texture TEXTURE = Texture.of(new Resource("textures/gui/widgets/context_menu.png"));

    private final List<ContextButton> actions = new ArrayList<>();
    private final int minWidth;
    private final int elementHeight;
    private boolean open;
    private Widget parent;
    private ContextMenu subContext;
    private int selected = -1;
    private boolean hovered;

    public ContextMenu() {
        this(0, 0);
    }

    public ContextMenu(int minWidth, int elementHeight) {
        super(0, 0, 0);
        this.minWidth = Math.max(minWidth, 22);
        this.elementHeight = Math.max(elementHeight, 12);
        this.setDimensions(this.minWidth, 6);
    }

    public boolean isOpen() {
        return open;
    }

    public boolean isHovered() {
        return isOpen() && (hovered || isSubContextHovered());
    }

    private boolean isSubContextHovered() {
        return subContext != null && subContext.isHovered();
    }

    public void close() {
        this.open = false;
        this.selected = -1;
    }

    public void open() {
        this.open = true;
        this.selected = -1;
    }

    public void setParent(Widget parent) {
        this.parent = parent;
    }

    public ContextMenu addAction(Text name, Text tooltip, Consumer<Button> action) {
        ContextButton button = new ContextButton(getX(), getAddY(), name, action, widgets.size(), this);
        button.setTooltip(tooltip);
        button.setDimensions(getWidthForText(name), elementHeight);

        this.addWidget(button);
        this.actions.add(button);

        return this;
    }

    public ContextMenu addDivider() {
        this.addWidget(new ContextDivider(getX(), getAddY(), getWidth(), widgets.size()));
        return this;
    }

    public ContextMenu addSubMenu(Text name, ContextMenu subContext) {
        ContextSubMenu subMenu = new ContextSubMenu(getX(), getAddY(), name, subContext, widgets.size(), this);
        subMenu.setDimensions(getWidthForText(name), elementHeight);

        this.addWidget(subMenu);

        return this;
    }

    private int getAddY() {
        return widgets.isEmpty() ? 0 : getHeight();
    }

    private int getWidthForText(Text name) {
        return Math.max(TextUtils.getWidth(name, Client.getInstance().font) + 4, minWidth - 2);
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
        matrices.translate(0f, 0f, parent instanceof ContextMenu ? 1f : 500f);

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
                32, 35
        );
    }

    @Override
    public GUIListener mousePress(int button, int action, int mods) {
        if (!isOpen())
            return null;

        //check if a child is being pressed first
        GUIListener sup = super.mousePress(button, action, mods);
        if (sup != null) return sup;

        //close context when clicked outside it, but do not void the mouse click
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
            default -> {super.keyPress(key, scancode, action, mods);}
        }

        return this;
    }

    @Override
    protected List<SelectableWidget> getSelectableWidgets() {
        return List.of();
    }

    private static void renderBackground(MatrixStack matrices, int x, int y, int width, int height, boolean hover, int index) {
        //bg
        VertexConsumer.GUI.consume(GeometryHelper.quad(matrices, x, y, width, height, (index % 2) * 16, 16f, 16, 16, 32, 35), TEXTURE.getID());

        //hover
        if (hover) UIHelper.nineQuad(
                VertexConsumer.GUI, matrices, TEXTURE.getID(),
                x, y,
                width, height,
                16f, 0f,
                16, 16,
                32, 35
        );
    }

    public static class ContextButton extends Button {
        protected final int index;
        protected final ContextMenu parent;

        public ContextButton(int x, int y, Text message, Consumer<Button> action, int index, ContextMenu parent) {
            super(x, y, 0, 0, message, action);
            this.index = index;
            this.parent = parent;
        }

        @Override
        public void renderWidget(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            super.renderWidget(matrices, mouseX, mouseY, delta);
            if (isHoveredOrFocused())
                parent.selected = index;
        }

        @Override
        protected void renderBackground(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            ContextMenu.renderBackground(matrices, getX(), getY(), getWidth(), getHeight(), isHoveredOrFocused(), index);
        }

        @Override
        protected void renderText(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            Text text = getFormattedMessage();
            Font f = Client.getInstance().font;
            int x = getX() + 2;
            int y = getCenterY() - TextUtils.getHeight(text, f) / 2;
            f.render(VertexConsumer.FONT, matrices, x, y, text);
        }

        @Override
        public boolean isHovered() {
            return super.isHovered() && !parent.isSubContextHovered();
        }
    }

    private static class ContextDivider extends Widget {
        private static final int HEIGHT = 5;
        private final int index;

        public ContextDivider(int x, int y, int width, int index) {
            super(x, y, width, HEIGHT);
            this.index = index;
        }

        @Override
        public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            ContextMenu.renderBackground(matrices, getX(), getY(), getWidth(), getHeight(), false, index);

            UIHelper.horizontalQuad(
                    VertexConsumer.GUI, matrices, TEXTURE.getID(),
                    getX() + 1, getY() + 1,
                    getWidth() - 2, 3,
                    0f, 32f,
                    32, 3,
                    32, 35
            );
        }
    }

    private static class ContextSubMenu extends ContextButton {
        private static final Text ARROW = Text.of("\u23F5");
        private final ContextMenu subContext;
        private float arrowOffset = 0f;

        public ContextSubMenu(int x, int y, Text message, ContextMenu subContext, int index, ContextMenu parent) {
            super(x, y, message, null, index, parent);
            this.subContext = subContext;
            subContext.setParent(parent);
        }

        @Override
        public void renderWidget(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            super.renderWidget(matrices, mouseX, mouseY, delta);

            //check for hover changes
            boolean hover = isHoveredOrFocused();
            if (hover && !subContext.isOpen()) {
                //set pos
                UIHelper.moveWidgetRelativeTo(this, subContext, 0);

                //add to parent
                parent.subContext = subContext;
                parent.listeners.addFirst(subContext);
                subContext.open();
            } else if (!hover && subContext.isOpen()) {
                //remove from parent
                parent.subContext = null;
                parent.listeners.remove(subContext);
                subContext.close();
            }

            //render subcontext if open
            if (subContext.isOpen())
                subContext.render(matrices, mouseX, mouseY, delta);
        }

        @Override
        protected void renderText(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            super.renderText(matrices, mouseX, mouseY, delta);

            //render arrow
            Font f = Client.getInstance().font;
            int x = getX() + getWidth() - 2 - TextUtils.getWidth(ARROW, f);
            int y = getCenterY() - TextUtils.getHeight(message, f) / 2;

            //arrow animation :3
            float d = UIHelper.tickDelta(0.6f);
            arrowOffset = Maths.lerp(arrowOffset, isHoveredOrFocused() ? 2f : 0f, d);

            f.render(VertexConsumer.FONT, matrices, x + arrowOffset, y, ARROW);
        }

        @Override
        public void onRun() {
            //do nothing
        }

        @Override
        public boolean isHoveredOrFocused() {
            return super.isHoveredOrFocused() || parent.selected == this.index;
        }
    }
}
