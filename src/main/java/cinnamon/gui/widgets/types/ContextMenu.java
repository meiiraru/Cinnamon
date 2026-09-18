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
import cinnamon.utils.Resource;
import cinnamon.utils.TextUtils;
import cinnamon.utils.UIHelper;
import org.joml.Math;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ContextMenu extends PopupWidget {

    private final WidgetList list = new WidgetList(0, 0, 0, 0, 0);
    private final List<Widget> widgets = new ArrayList<>();

    private final int minWidth;
    private final int elementHeight;
    private ContextMenu subContext;
    private int selected = -1;

    private int totalWidth;
    private int totalHeight = 0;

    public ContextMenu() {
        this(0, 0);
    }

    public ContextMenu(int minWidth, int elementHeight) {
        super(0, 0, 0);
        this.minWidth = this.totalWidth = Math.max(minWidth, 22);
        this.elementHeight = Math.max(12, elementHeight);
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
        for (Widget widget : widgets) {
            if (widget instanceof Button && ((Button) widget).isHolding())
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
        return addAction(new ContextButton(getWidthForText(name), elementHeight, name, tooltip, action, widgets.size(), this));
    }

    public ContextMenu addDivider() {
        return addDivider(false);
    }

    public ContextMenu addDivider(boolean hidden) {
        return addAction(new ContextDivider(totalWidth, hidden, getSkin().getInt("context_menu_divider_size")), true);
    }

    public ContextMenu addSubMenu(Text name, ContextMenu subContext) {
        return addAction(new ContextSubMenu(getWidthForText(name), elementHeight, name, subContext, widgets.size(), this));
    }

    public ContextMenu addAction(Widget widget) {
        return addAction(widget, false);
    }

    public ContextMenu addAction(Widget widget, boolean divider) {
        list.addWidget(widget);
        if (!divider)
            widgets.add(widget);

        totalHeight += widget.getHeight();
        totalWidth = Math.max(totalWidth, widget.getWidth());
        setDimensions(totalWidth, totalHeight);

        return this;
    }

    public void clearActions() {
        for (Widget widget : widgets)
            list.removeWidget(widget);
        widgets.clear();
        totalWidth = minWidth;
        totalHeight = 0;
    }

    private int getWidthForText(Text name) {
        return Math.max(TextUtils.getWidth(name) + 4, minWidth - 2);
    }

    public Widget getAction(int i) {
        return widgets.get(i);
    }

    public void scrollToAction(int i) {
        if (i >= 0 && i < widgets.size())
            list.scrollToWidget(widgets.get(i));
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
        list.forceUpdate();

        //check for scrollbar
        int scroll = list.shouldRenderScrollbar() ? list.getScrollbarWidth() + 1 : 0;

        //get new width
        int w = Math.min(width, realWidth + scroll);

        //set new width
        list.setWidth(w);
        setWidth(w);

        //apply new width to all widgets, without the scroll
        for (Widget widget : widgets)
            widget.setWidth(w - scroll);
    }

    @Override
    public void renderWidget(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        Resource background = getSkin().getResource("context_menu_tex");

        //render background
        matrices.pushMatrix();
        matrices.translate(0f, 0f, -UIHelper.getDepthOffset());
        UIHelper.nineQuad(
                VertexConsumer.MAIN, matrices, background,
                getX() - 1, getY() - 1,
                getWidth() + 2, getHeight() + 2,
                0f, 0f,
                16, 16,
                32, 35
        );
        matrices.popMatrix();

        List<Widget> widgetList = this.widgets;
        for (int i = 0; i < widgetList.size(); i++) {
            Widget widget = widgetList.get(i);
            ContextMenu.renderBackground(matrices, widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight(), i, background);
        }
    }

    private static void renderBackground(MatrixStack matrices, int x, int y, int width, int height, int index, Resource texture) {
        //bg
        VertexConsumer.MAIN.consume(GeometryHelper.quad(matrices, x, y, width, height, (index % 2) * 16, 16f, 16, 16, 32, 35), texture);
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
            //hover
            if (isHoveredOrFocused()) {
                matrices.pushMatrix();
                matrices.translate(0f, 0f, UIHelper.getDepthOffset());
                UIHelper.nineQuad(
                        VertexConsumer.MAIN, matrices, getSkin().getResource("context_menu_tex"),
                        getX(), getY(),
                        getWidth(), getHeight(),
                        16f, 0f,
                        16, 16,
                        32, 35
                );
                matrices.popMatrix();
            }
        }

        @Override
        protected void renderText(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            Text text = getFormattedMessage();
            int x = getX() + 2;
            int y = getCenterY();
            text.render(VertexConsumer.MAIN, matrices, x, y, Alignment.CENTER_LEFT);
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
            boolean hovered = UIHelper.isMouseOver(getX(), getY(), getWidth(), getHeight(), x, y);
            setHovered(hovered);
        }
    }

    private static class ContextDivider extends Widget {
        public ContextDivider(int width, boolean hidden, int height) {
            super(0, 0, width, height);
            this.setVisible(!hidden);
        }

        @Override
        public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            if (!isVisible())
                return;

            matrices.pushMatrix();
            matrices.translate(0f, 0f, UIHelper.getDepthOffset());
            UIHelper.horizontalQuad(
                    VertexConsumer.MAIN, matrices, getSkin().getResource("context_menu_tex"),
                    getX() + 1, Math.round(getCenterY() - 1.5f),
                    getWidth() - 2, 3,
                    0f, 32f,
                    32, 3,
                    32, 35
            );
            matrices.popMatrix();
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
            Text arrow = Text.empty().withStyle(Style.EMPTY.guiSkin(getSkinRes())).append(ARROW);
            int x = getX() + getWidth() - 2;
            int y = getCenterY();

            //arrow animation :3
            float d = UIHelper.tickDelta(0.6f);
            arrowOffset = Math.lerp(arrowOffset, subContext.isOpen() ? 2f : 0f, d);

            arrow.render(VertexConsumer.MAIN, matrices, x + arrowOffset, y, Alignment.CENTER_RIGHT);
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
    }
}
