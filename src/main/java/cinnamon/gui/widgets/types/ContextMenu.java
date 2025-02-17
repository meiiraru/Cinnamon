package cinnamon.gui.widgets.types;

import cinnamon.gui.widgets.GUIListener;
import cinnamon.gui.widgets.PopupWidget;
import cinnamon.gui.widgets.Widget;
import cinnamon.gui.widgets.WidgetList;
import cinnamon.model.GeometryHelper;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.text.Style;
import cinnamon.text.Text;
import cinnamon.utils.Alignment;
import cinnamon.utils.Maths;
import cinnamon.utils.Resource;
import cinnamon.utils.TextUtils;
import cinnamon.utils.UIHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ContextMenu extends PopupWidget {

    private final WidgetList list = new WidgetList(0, 0, 0, 0, 0);
    private final List<ContextButton> actions = new ArrayList<>();
    private final List<Widget> widgets = new ArrayList<>();

    private final int minWidth;
    private final int elementHeight;
    private ContextMenu subContext;
    private int selected = -1;

    private int totalWidth;
    private int totalHeight = 2;

    public ContextMenu() {
        this(0, 0);
    }

    public ContextMenu(int minWidth, int elementHeight) {
        super(0, 0, 0);
        this.minWidth = this.totalWidth = Math.max(minWidth, 22);
        this.elementHeight = Math.max(elementHeight, 12);
        list.setDimensions(this.minWidth, this.elementHeight);
        list.setAlignment(Alignment.TOP_LEFT);
        list.setIgnoreScrollbarOffset(true);
        addWidget(list);
    }

    @Override
    public boolean isHovered() {
        return isOpen() && (super.isHovered() || isSubContextHovered() || isHoldingChild());
    }

    private boolean isSubContextHovered() {
        return subContext != null && subContext.isHovered();
    }

    protected boolean isHoldingChild() {
        for (ContextButton action : actions) {
            if (action.isHolding())
                return true;
        }
        return false;
    }

    @Override
    protected void reset() {
        super.reset();
        this.selected = -1;
        list.scrollToTop();
    }

    public ContextMenu addAction(Text name, Text tooltip, Consumer<Button> action) {
        ContextButton button = new ContextButton(getWidthForText(name), elementHeight, name, tooltip, action, widgets.size(), this);
        this.actions.add(button);
        addAction(button);
        return this;
    }

    public ContextMenu addDivider() {
        addAction(new ContextDivider(totalWidth, getStyle().dividerSize, widgets.size()));
        return this;
    }

    public ContextMenu addSubMenu(Text name, ContextMenu subContext) {
        addAction(new ContextSubMenu(getWidthForText(name), elementHeight, name, subContext, widgets.size(), this));
        return this;
    }

    private void addAction(Widget widget) {
        list.addWidget(widget);
        widgets.add(widget);

        totalHeight += widget.getHeight();
        totalWidth = Math.max(totalWidth, widget.getWidth());
        setDimensions(totalWidth, totalHeight);
    }

    public void clearActions() {
        for (ContextButton action : actions)
            list.removeWidget(action);
        widgets.clear();
        actions.clear();
        totalWidth = minWidth;
        totalHeight = 2;
    }

    private int getWidthForText(Text name) {
        return Math.max(TextUtils.getWidth(name) + 4, minWidth - 2);
    }

    public Button getAction(int i) {
        return actions.get(i);
    }

    @Override
    public void fitToScreen(int width, int height) {
        //reset dimensions
        setDimensions(totalWidth, totalHeight);

        //call super
        super.fitToScreen(width, height);

        //grab new values
        int realWidth = getWidth();
        int realHeight = getHeight();

        //set list height
        list.setHeight(realHeight);

        //check for scrollbar
        int scroll = list.shouldRenderScrollbar() ? list.getScrollbarWidth() + 1 : 0;

        //get new width
        int w = Math.min(realWidth + scroll, width);

        //set new width
        list.setWidth(w);
        setWidth(w);

        //apply new width to all widgets, without the scroll
        for (Widget widget : widgets)
            widget.setWidth(w - scroll);
    }

    @Override
    public void renderWidget(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        //render background
        UIHelper.nineQuad(
                VertexConsumer.GUI, matrices, getStyle().contextMenuTex,
                getX() - 1, getY() - 1,
                getWidth() + 2, getHeight() + 2,
                0f, 0f,
                16, 16,
                32, 35
        );
    }

    private static void renderBackground(MatrixStack matrices, int x, int y, int width, int height, boolean hover, int index, Resource texture) {
        //bg
        VertexConsumer.GUI.consume(GeometryHelper.quad(matrices, x, y, width, height, (index % 2) * 16, 16f, 16, 16, 32, 35), texture);

        //hover
        if (hover) UIHelper.nineQuad(
                VertexConsumer.GUI, matrices, texture,
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
            ContextMenu.renderBackground(matrices, getX(), getY(), getWidth(), getHeight(), isHoveredOrFocused(), index, getStyle().contextMenuTex);
        }

        @Override
        protected void renderText(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            Text text = getFormattedMessage();
            int x = getX() + 2;
            int y = getCenterY();
            text.render(VertexConsumer.FONT, matrices, x, y, Alignment.CENTER_LEFT);
        }

        @Override
        public boolean isHovered() {
            return super.isHovered() && !parent.isSubContextHovered();
        }

        @Override
        public void setRunOnHold(boolean bool) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void updateHover(int x, int y) {
            setHovered(UIHelper.isMouseOver(getX(), getY(), getWidth(), getHeight(), x, y));
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
            ContextMenu.renderBackground(matrices, getX(), getY(), getWidth(), getHeight(), false, index, getStyle().contextMenuTex);

            UIHelper.horizontalQuad(
                    VertexConsumer.GUI, matrices, getStyle().contextMenuTex,
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
            Text arrow = Text.empty().withStyle(Style.EMPTY.guiStyle(getStyleRes())).append(ARROW);
            int x = getX() + getWidth() - 2;
            int y = getCenterY();

            //arrow animation :3
            float d = UIHelper.tickDelta(0.6f);
            arrowOffset = Maths.lerp(arrowOffset, isHoveredOrFocused() ? 2f : 0f, d);

            arrow.render(VertexConsumer.FONT, matrices, x + arrowOffset, y, Alignment.CENTER_RIGHT);
        }

        @Override
        public void onRun() {
            //do nothing
        }

        @Override
        public boolean isHoveredOrFocused() {
            return super.isHoveredOrFocused() || parent.selected == this.index;
        }

        @Override
        public GUIListener mousePress(int button, int action, int mods) {
            return null;
        }

        @Override
        public void setStyle(Resource style) {
            super.setStyle(style);
            subContext.setStyle(style);
        }
    }
}
