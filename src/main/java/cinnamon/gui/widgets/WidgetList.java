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

import static org.lwjgl.glfw.GLFW.GLFW_PRESS;

public class WidgetList extends ContainerGrid {

    private static final Resource TEXTURE = new Resource("textures/gui/widgets/list.png");

    private final List<Widget> widgetsToRender = new ArrayList<>();

    private final Scrollbar scrollbar;
    private boolean showScrollbar = true;
    private boolean shouldRenderScrollbar;
    private boolean shouldRenderBackground = true;
    private int widgetsWidth, widgetsHeight;
    private int lastY;

    public WidgetList(int x, int y, int width, int height, int spacing) {
        this(x, y, width, height, spacing, 1);
    }

    public WidgetList(int x, int y, int width, int height, int spacing, int columns) {
        super(x, y, spacing, columns);
        setDimensions(width, height);
        setAlignment(Alignment.CENTER);

        scrollbar = new Scrollbar(0, 0, height - 2);
        listeners.add(scrollbar);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        if (shouldRenderBackground)
            renderBackground(matrices, mouseX, mouseY, delta);

        boolean scroll = shouldRenderScrollbar;
        if (scroll)
            UIHelper.pushScissors(getAlignedX(), getY(), getWidth(), getHeight());

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
                getAlignedX() - 1, getY() - 1,
                getWidth() + 2, getHeight() + 2,
                0f, 0f,
                16, 16,
                16, 16
        );
    }

    @Override
    public void updateDimensions() {
        super.updateDimensions();

        //check if size is widget list is bigger than container height
        shouldRenderScrollbar = getWidgetsHeight() - getHeight() > 0;

        //spacing
        int width = getWidth();
        if (shouldRenderScrollbar) width -= scrollbar.getWidth() + 3;
        int x = -Math.round(alignment.getOffset(width - getWidgetsWidth()));
        for (Widget widget : widgets)
            widget.translate(x, 1);

        //update scrollbar
        if (scrollbar != null) {
            scrollbar.setHandlePercentage((float) scrollbar.getHeight() / getWidgetsHeight());

            updateList();
            int remaining = widgets.size() - widgetsToRender.size();

            scrollbar.setScrollAmount(1f / remaining);
            scrollbar.setMax(remaining);
        }
    }

    @Override
    protected void updateDimensions(int width, int height) {
        //do not update dimensions here, but save them
        widgetsWidth = width;
        widgetsHeight = height;
    }

    protected List<Widget> updateList() {
        //all widgets are allowed to render
        if (!shouldRenderScrollbar)
            return widgets;

        //clear render list
        widgetsToRender.clear();

        //grab height difference
        int newY = Math.round((getWidgetsHeight() - getHeight()) * scrollbar.getAnimationValue());
        int diff = lastY - newY;
        lastY = newY;

        int thisY = this.getY();
        int thisY2 = thisY + this.getHeight();
        boolean hovered = this.isHovered();
        Window w = Client.getInstance().window;

        //apply new scroll
        for (Widget widget : widgets) {
            //set y based on scrollbar
            int y = widget.getY() + diff;
            widget.setY(y);

            //if widget is inside list, allow it for render
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
        //not initialized yet
        if (scrollbar == null)
            return;

        scrollbar.setHeight(getHeight() - 2);
        scrollbar.setPos(
                getAlignedX() + getWidth() - scrollbar.getWidth() - 1,
                getY() + 1
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
    public GUIListener mousePress(int button, int action, int mods) {
        return isHovered() || action != GLFW_PRESS ? super.mousePress(button, action, mods) : null;
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
        if (shouldRenderScrollbar) sup.addFirst(scrollbar);
        return sup;
    }

    public boolean isHovered() {
        Window w = Client.getInstance().window;
        return UIHelper.isMouseOver(getAlignedX(), getY(), getWidth(), getHeight(), w.mouseX, w.mouseY);
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

    public void setShouldRenderBackground(boolean shouldRenderBackground) {
        this.shouldRenderBackground = shouldRenderBackground;
    }

    public int getWidgetsWidth() {
        return widgetsWidth;
    }

    public int getWidgetsHeight() {
        return widgetsHeight + 2; //include spacing
    }
}
