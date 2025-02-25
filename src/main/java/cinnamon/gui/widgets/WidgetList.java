package cinnamon.gui.widgets;

import cinnamon.Client;
import cinnamon.gui.widgets.types.Scrollbar;
import cinnamon.render.MatrixStack;
import cinnamon.render.Window;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.utils.Alignment;
import cinnamon.utils.Resource;
import cinnamon.utils.UIHelper;

import java.util.ArrayList;
import java.util.List;

public class WidgetList extends ContainerGrid {

    private static final Resource TEXTURE = new Resource("textures/gui/widgets/container_background.png");

    private final List<Widget> widgetsToRender = new ArrayList<>();

    private final Scrollbar scrollbar;
    private boolean showScrollbar = true;
    private boolean hasBackground;
    private boolean ignoreScrollbarOffset;
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
        if (hasBackground)
            renderBackground(matrices, mouseX, mouseY, delta);

        boolean scroll = shouldRenderScrollbar();
        if (scroll)
            UIHelper.pushScissors(getAlignedX(), getAlignedY(), getWidth(), getHeight());

        for (Widget widget : updateList())
            widget.render(matrices, mouseX, mouseY, delta);

        if (scroll) {
            if (showScrollbar)
                scrollbar.render(matrices, mouseX, mouseY, delta);
            UIHelper.popScissors();
        }
    }

    protected void renderBackground(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        //render background
        UIHelper.nineQuad(
                VertexConsumer.GUI, matrices, TEXTURE,
                getAlignedX() - 1, getAlignedY() - 1,
                getWidth() + 2, getHeight() + 2,
                0f, 0f,
                16, 16,
                16, 16
        );
    }

    @Override
    protected void updateDimensions() {
        lastY = 0;
        super.updateDimensions();

        //scrollbar limit
        if (!ignoreScrollbarOffset && shouldRenderScrollbar()) {
            //grab the widgets initial X point
            int x = getX() + widgetsWidth + Math.round(alignment.getWidthOffset(widgetsWidth));
            if (x > scrollbar.getX() - 1) {
                x -= scrollbar.getX() - 1;
                for (Widget widget : widgets)
                    widget.translate(-x, 1);
            }
        } else {
            for (Widget widget : widgets)
                widget.translate(0, 1);
        }

        //update scrollbar
        updateList();
        scrollbar.setScrollAmount(3f / widgets.size()); //3 widgets per scroll
        scrollbar.setHandleSize((int) (scrollbar.getHeight() * getHeight() / (float) getWidgetsHeight()));

        updateScrollbar();
    }

    @Override
    protected void updateDimensions(int width, int height) {
        //do not update dimensions here, but save them
        widgetsWidth = width;
        widgetsHeight = height;
    }

    protected List<Widget> updateList() {
        //all widgets are allowed to render
        if (getWidgetsHeight() <= getHeight())
            return widgets;

        //clear render list
        widgetsToRender.clear();

        //grab height difference
        int heightDiff = getWidgetsHeight() - getHeight();
        int newY = Math.round(alignment.getHeightOffset(heightDiff) + heightDiff * scrollbar.getAnimationValue());
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
    protected List<SelectableWidget> getSelectableWidgets() {
        List<SelectableWidget> sup = super.getSelectableWidgets();
        if (shouldRenderScrollbar()) sup.addFirst(scrollbar);
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

    public void setBackground(boolean bool) {
        this.hasBackground = bool;
    }

    public int getWidgetsWidth() {
        return widgetsWidth;
    }

    public int getWidgetsHeight() {
        return widgetsHeight + 2; //include spacing
    }

    public int getScrollbarWidth() {
        return scrollbar.getWidth();
    }

    public void scrollToTop() {
        scrollbar.setPercentage(0);
    }

    public boolean shouldRenderScrollbar() {
        return showScrollbar && getWidgetsHeight() > getHeight();
    }

    public void setIgnoreScrollbarOffset(boolean bool) {
        this.ignoreScrollbarOffset = bool;
    }
}
