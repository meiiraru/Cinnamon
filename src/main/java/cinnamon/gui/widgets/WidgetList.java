package cinnamon.gui.widgets;

import cinnamon.Client;
import cinnamon.gui.widgets.types.Scrollbar;
import cinnamon.render.MatrixStack;
import cinnamon.render.Window;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.utils.Alignment;
import cinnamon.utils.UIHelper;
import org.joml.Math;

import java.util.ArrayList;
import java.util.List;

public class WidgetList extends ContainerGrid {

    private final List<Widget> widgetsToRender = new ArrayList<>();

    private final Scrollbar scrollbar;
    private boolean showScrollbar = true;
    private int scrollPadding = 1, maxScrollAmount = 36; //px
    private boolean ignoreScrollbarOffset;
    private boolean allowTabNavigation = true;
    private int widgetsWidth, widgetsHeight;
    private int lastY;

    public WidgetList(int x, int y, int width, int height, int spacing) {
        this(x, y, width, height, spacing, 1);
    }

    public WidgetList(int x, int y, int width, int height, int spacing, int columns) {
        super(x, y, spacing, columns);
        scrollbar = new Scrollbar(0, 0, height - 2);
        scrollbar.setParent(this);
        setDimensions(width, height);
        setAlignment(Alignment.TOP_CENTER);
        listeners.add(scrollbar);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        if (hasBackground())
            renderBackground(matrices, mouseX, mouseY, delta);

        boolean scroll = isScrollbarNeeded();
        if (scroll)
            UIHelper.pushStencil(matrices, getAlignedX(), getAlignedY(), getWidth(), getHeight());

        for (Widget widget : updateList())
            widget.render(matrices, mouseX, mouseY, delta);

        if (scroll) {
            if (shouldRenderScrollbar())
                scrollbar.render(matrices, mouseX, mouseY, delta);
            UIHelper.popStencil();
        }
    }

    @Override
    protected void renderBackground(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        //render background
        UIHelper.nineQuad(
                VertexConsumer.MAIN, matrices, getSkin().getResource("container_background_tex"),
                getAlignedX(), getAlignedY(),
                getWidth(), getHeight(),
                0f, 0f,
                16, 16,
                16, 16
        );
    }

    @Override
    public void clear() {
        this.widgets.clear();
        this.listeners.clear();
        //width and height are not reset
        if (this.showScrollbar)
            this.listeners.add(this.scrollbar);
    }

    @Override
    protected void updateDimensions() {
        lastY = 0;
        super.updateDimensions();

        //update scrollbar position first
        updateScrollbar();

        //scrollbar x offset
        if (!ignoreScrollbarOffset() && shouldRenderScrollbar()) {
            //grab the widgets initial X point
            int x = getX() + widgetsWidth + Math.round(alignment.getWidthOffset(widgetsWidth));
            if (x > scrollbar.getX() - 1) {
                x -= scrollbar.getX() - 1;
                for (Widget widget : widgets)
                    widget.translate(-x, 0);
            }
        }

        //update scrollbar
        updateList();

        float scrollAmount = Math.min(getHeight(), maxScrollAmount);
        scrollbar.setScrollAmount(Math.min(scrollAmount / (float) (getWidgetsHeight() - getHeight()), 1f));
        if (getWidgetsHeight() > 0)
            scrollbar.setHandleSize((int) (scrollbar.getHeight() * getHeight() / (float) getWidgetsHeight()));
    }

    @Override
    protected void updateDimensions(int width, int height) {
        //do not update dimensions here, but save them
        widgetsWidth = width;
        widgetsHeight = height;
    }

    protected List<Widget> updateList() {
        //all widgets are allowed to render
        if (!isScrollbarNeeded())
            return widgets;

        //clear render list
        widgetsToRender.clear();

        //grab height difference
        int paddingOffset = Math.round(alignment.getHeightOffset(scrollPadding * 2f) + scrollPadding);
        int heightDiff = (getWidgetsHeight() + scrollPadding * 2) - getHeight();
        int newY = Math.round(alignment.getHeightOffset(heightDiff) + heightDiff * (showScrollbar() ? scrollbar.getAnimationValue() : scrollbar.getPercentage())) - paddingOffset;
        int diff = lastY - newY;
        lastY = newY;

        int thisY = this.getAlignedY();
        int thisY2 = thisY + this.getHeight();
        boolean hovered = this.isHovered();
        Window w = Client.getInstance().window;

        //apply new scroll
        for (Widget widget : widgets) {
            //set y based on scrollbar
            int y = widget.getY() + diff;
            widget.setY(y);

            //if widget is inside list, allow it for render
            if (widget instanceof AlignedWidget aw)
                y += Math.round(aw.getAlignment().getHeightOffset(widget.getHeight()));

            boolean isInside = y + widget.getHeight() >= thisY && y <= thisY2;
            if (isInside)
                widgetsToRender.add(widget);

            //update hover status
            if (widget instanceof SelectableWidget sw) {
                if (!isInside || !hovered)
                    sw.setHovered(false);
                else
                    sw.updateHover(w.mouseX, w.mouseY);
            }
        }

        return widgetsToRender;
    }

    protected void updateScrollbar() {
        scrollbar.setHeight(getHeight() - 2);
        scrollbar.setPos(
                getAlignedX() + getWidth() - getScrollbarWidth() - 1,
                getAlignedY() + 1
        );
    }

    public void scrollToWidget(int index) {
        if (index >= 0 && index < widgets.size())
            scrollToWidget(widgets.get(index));
    }

    public void scrollToWidget(Widget widget) {
        if (widget == null)
            return;

        //check if the widget is a child of this list
        boolean isDescendant = false;
        Widget p = widget;
        while (p != null) {
            if (p == this) {
                isDescendant = true;
                break;
            }
            p = p.getParent();
        }

        if (!isDescendant)
            return;

        int widgetY = widget.getY();
        int widgetHeight = widget.getHeight();
        int widgetY2 = widgetY + widgetHeight;

        int thisY = getAlignedY();
        int thisHeight = getHeight();
        int thisY2 = thisY + thisHeight;

        float scrollHeight = getWidgetsHeight() - thisHeight;
        if (scrollHeight <= 0)
            return;

        float scrollPercent = scrollbar.getAnimationValue();

        //scroll up
        if (widgetY < thisY)
            scrollPercent += (widgetY - thisY) / scrollHeight;
        //scroll down
        else if (widgetY2 > thisY2)
            scrollPercent += (widgetY2 - thisY2) / scrollHeight;

        scrollbar.setPercentage(scrollPercent);
    }

    @Override
    public void setX(int x) {
        super.setX(x);
        updateScrollbar();
    }

    @Override
    public void setY(int y) {
        super.setY(y);
        updateScrollbar();
    }

    @Override
    public void setWidth(int width) {
        super.setWidth(width);
        updateScrollbar();
    }

    @Override
    public void setHeight(int height) {
        super.setHeight(height);
        updateScrollbar();
    }

    @Override
    public GUIListener scroll(double x, double y) {
        GUIListener sup = super.scroll(x, y);
        if (sup != null)
            return sup;

        if (isHovered())
            return scrollbar.forceScroll(x, y);

        return null;
    }

    @Override
    protected List<SelectableWidget> getSelectableWidgets(boolean isTab) {
        List<SelectableWidget> sup = super.getSelectableWidgets(isTab);

        //if tab navigation is not allowed, allow only the first widget to be selected, and do not allow to select any other widget by tabbing
        if (isTab && !allowTabNavigation) {
            if (!sup.isEmpty())
                return List.of(sup.getFirst());
            else
                return List.of();
        }

        //if (shouldRenderScrollbar())
        //    sup.addFirst(scrollbar);

        return sup;
    }

    public boolean isHovered() {
        Window w = Client.getInstance().window;
        return UIHelper.isMouseOver(getAlignedX(), getAlignedY(), getWidth(), getHeight(), w.mouseX, w.mouseY);
    }

    public void setShowScrollbar(boolean showScrollbar) {
        if (this.showScrollbar != showScrollbar) {
            if (showScrollbar) {
                listeners.addFirst(scrollbar);
            } else {
                listeners.remove(scrollbar);
            }
            this.showScrollbar = showScrollbar;
        }
    }

    public boolean showScrollbar() {
        return showScrollbar;
    }

    public int getWidgetsWidth() {
        return widgetsWidth;
    }

    public int getWidgetsHeight() {
        return widgetsHeight;
    }

    public int getScrollbarWidth() {
        return scrollbar.getWidth();
    }

    public void scrollToTop() {
        scrollbar.setPercentage(0f);
    }

    public void scrollToBottom() {
        scrollbar.setPercentage(1f);
    }

    public boolean shouldRenderScrollbar() {
        return showScrollbar && isScrollbarNeeded();
    }

    public boolean isScrollbarNeeded() {
        return getWidgetsHeight() > getHeight();
    }

    public void setIgnoreScrollbarOffset(boolean bool) {
        this.ignoreScrollbarOffset = bool;
    }

    public boolean ignoreScrollbarOffset() {
        return ignoreScrollbarOffset;
    }

    public void setAllowTabNavigation(boolean allowTabNavigation) {
        this.allowTabNavigation = allowTabNavigation;
    }

    public boolean allowTabNavigation() {
        return allowTabNavigation;
    }

    public int getScrollPadding() {
        return scrollPadding;
    }

    public void setScrollPadding(int scrollPadding) {
        this.scrollPadding = scrollPadding;
    }

    public int getMaxScrollAmount() {
        return maxScrollAmount;
    }

    public void setMaxScrollAmount(int maxScrollAmount) {
        this.maxScrollAmount = maxScrollAmount;
    }

    public Scrollbar getScrollbar() {
        return scrollbar;
    }
}