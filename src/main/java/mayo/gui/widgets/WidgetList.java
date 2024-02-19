package mayo.gui.widgets;

import mayo.Client;
import mayo.gui.widgets.types.Scrollbar;
import mayo.render.MatrixStack;
import mayo.render.Texture;
import mayo.render.Window;
import mayo.render.batch.VertexConsumer;
import mayo.utils.Alignment;
import mayo.utils.Resource;
import mayo.utils.UIHelper;

import java.util.ArrayList;
import java.util.List;

public class WidgetList extends ContainerGrid {

    private static final Texture TEXTURE = Texture.of(new Resource("textures/gui/widgets/list.png"));

    private final List<Widget> widgetsToRender = new ArrayList<>();

    private final Scrollbar scrollbar;
    private boolean showScrollbar = true;
    private boolean shouldRenderScrollbar;
    private boolean shouldRenderBackground = true;
    private int widgetsWidth, widgetsHeight;

    public WidgetList(int x, int y, int width, int height, int spacing) {
        super(x, y, spacing, 1);
        setDimensions(width, height);
        setAlignment(Alignment.CENTER);

        scrollbar = new Scrollbar(0, 0, height - 2);
        scrollbar.setChangeListener((f, i) -> updateDimensions());
        listeners.add(scrollbar);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        if (shouldRenderBackground)
            renderBackground(matrices, mouseX, mouseY, delta);

        boolean scroll = shouldRenderScrollbar;
        if (scroll)
            UIHelper.pushScissors(getAlignedX(), getY(), getWidth(), getHeight());

        for (Widget widget : widgetsToRender)
            widget.render(matrices, mouseX, mouseY, delta);

        if (scroll) {
            scrollbar.render(matrices, mouseX, mouseY, delta);
            UIHelper.popScissors();
        }
    }

    protected void renderBackground(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        //render background
        UIHelper.nineQuad(
                VertexConsumer.GUI, matrices, TEXTURE.getID(),
                getAlignedX() - 1, getY() - 1,
                getWidth() + 2, getHeight() + 2,
                0f, 0f,
                16, 16,
                16, 16
        );
    }

    @Override
    public void updateDimensions() { //todo
        super.updateDimensions();

        //clear render list
        widgetsToRender.clear();

        //grab height difference, also account for borders spacing
        int diff = getWidgetsHeight() - getHeight();
        if (diff > 0) {
            int d = (int) (diff * scrollbar.getPercentage());
            for (Widget widget : widgets) {
                //set y based on scrollbar and borders spacing
                int y = widget.getY() - d;
                widget.setY(y);

                //update hover status
                if (widget instanceof SelectableWidget sw) {
                    Window w = Client.getInstance().window;
                    sw.updateHover(w.mouseX, w.mouseY);
                }

                //if widget is inside list, allow it for render
                if (y + widget.getHeight() >= this.getY() && y <= this.getY() + this.getHeight())
                    widgetsToRender.add(widget);
            }
        } else {
            //all widgets are allowed to render
            widgetsToRender.addAll(widgets);
        }

        //update scrollbar
        shouldRenderScrollbar = showScrollbar && diff > 0;
        updateScrollbar();

        //fix spacing
        int width = getWidth();
        if (shouldRenderScrollbar) width -= scrollbar.getWidth() + 3;
        int x = 1 - Math.round(alignment.getOffset(width - getWidgetsWidth()));
        for (Widget widget : widgets)
            widget.translate(x, 1);
    }

    @Override
    protected void updateDimensions(int width, int height) {
        //do not update dimensions here, but save them
        widgetsWidth = width;
        widgetsHeight = height;
    }

    protected void updateScrollbar() {
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
                updateScrollbar();
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
