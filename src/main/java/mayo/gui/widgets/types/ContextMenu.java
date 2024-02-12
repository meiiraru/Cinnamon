package mayo.gui.widgets.types;

import mayo.Client;
import mayo.gui.widgets.PopupWidget;
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

public class ContextMenu extends PopupWidget {

    private static final Texture TEXTURE = Texture.of(new Resource("textures/gui/widgets/context_menu.png"));
    private static final int DIVIDER_HEIGHT = 5;

    private final List<ContextButton> actions = new ArrayList<>();
    private final int minWidth;
    private final int elementHeight;
    private ContextMenu subContext;
    private int selected = -1;

    public ContextMenu() {
        this(0, 0);
    }

    public ContextMenu(int minWidth, int elementHeight) {
        super(0, 0, 0);
        this.minWidth = Math.max(minWidth, 22);
        this.elementHeight = Math.max(elementHeight, 12);
        this.setDimensions(this.minWidth, 6);
    }

    @Override
    public boolean isHovered() {
        return isOpen() && (super.isHovered() || isSubContextHovered());
    }

    private boolean isSubContextHovered() {
        return subContext != null && subContext.isHovered();
    }

    @Override
    protected void reset() {
        super.reset();
        this.selected = -1;
    }

    public ContextMenu addAction(Text name, Text tooltip, Consumer<Button> action) {
        ContextButton button = new ContextButton(getWidthForText(name), elementHeight, name, tooltip, action, widgets.size(), this);
        this.addWidget(button);
        this.actions.add(button);
        return this;
    }

    public ContextMenu addDivider() {
        this.addWidget(new ContextDivider(getWidth(), DIVIDER_HEIGHT, widgets.size()));
        return this;
    }

    public ContextMenu addSubMenu(Text name, ContextMenu subContext) {
        this.addWidget(new ContextSubMenu(getWidthForText(name), elementHeight, name, subContext, widgets.size(), this));
        return this;
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
    public void renderWidget(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        //render background
        UIHelper.nineQuad(
                VertexConsumer.GUI, matrices, TEXTURE.getID(),
                getX() - 1, getY() - 1,
                getWidth() + 2, getHeight() + 2,
                0f, 0f,
                16, 16,
                32, 35
        );
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

        public ContextButton(int width, int height, Text message, Text tooltip, Consumer<Button> action, int index, ContextMenu parent) {
            super(0, 0, width, height, message, action);
            setTooltip(tooltip);
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
        private final int index;
        public ContextDivider(int width, int height, int index) {
            super(0, 0, width, height);
            this.index = index;
        }

        @Override
        public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            ContextMenu.renderBackground(matrices, getX(), getY(), getWidth(), getHeight(), false, index);

            UIHelper.horizontalQuad(
                    VertexConsumer.GUI, matrices, TEXTURE.getID(),
                    getX() + 1, Math.round(getCenterY() - 1.5f),
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

        public ContextSubMenu(int width, int height, Text message, ContextMenu subContext, int index, ContextMenu parent) {
            super(width, height, message, null, null, index, parent);
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
