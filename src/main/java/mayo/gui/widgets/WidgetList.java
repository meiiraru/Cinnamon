package mayo.gui.widgets;

import mayo.Client;
import mayo.gui.widgets.types.Scrollbar;
import mayo.render.MatrixStack;
import mayo.render.Texture;
import mayo.render.Window;
import mayo.render.batch.VertexConsumer;
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

    private int widgetsHeight;

    public WidgetList(int x, int y, int width, int height, int spacing) {
        super(x, y, spacing, 1);
        setDimensions(width, height);

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
            UIHelper.pushScissors(getX(), getY(), getWidth(), getHeight());

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
                getX() - 1, getY() - 1,
                getWidth() + 2, getHeight() + 2,
                0f, 0f,
                16, 16,
                16, 16
        );
    }

    @Override
    public void updateDimensions() { //todo
        super.updateDimensions();

        widgetsToRender.clear();

        int diff = widgetsHeight - getHeight();
        if (diff > 0) {
            int d = (int) (diff * scrollbar.getPercentage());
            for (Widget widget : widgets) {
                int y = widget.getY() - d;
                widget.setY(y);

                if (widget instanceof SelectableWidget sw) {
                    Window w = Client.getInstance().window;
                    sw.updateHover(w.mouseX, w.mouseY);
                }

                if (y + widget.getHeight() >= this.getY() && y <= this.getY() + this.getHeight())
                    widgetsToRender.add(widget);
            }
        } else {
            widgetsToRender.addAll(widgets);
        }

        shouldRenderScrollbar = true;
        updateScrollbar();
    }

    @Override
    protected void updateDimensions(int width, int height) {
        //do not update dimensions here, but save the height
        widgetsHeight = height;
    }

    protected void updateScrollbar() {
        scrollbar.setPos(
                getX() + getWidth() - scrollbar.getWidth() - 1,
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
    public GUIListener scroll(double x, double y) {
        GUIListener sup = super.scroll(x, y);
        if (sup != null)
            return sup;

        if (isHovered())
            return scrollbar.forceScroll(x, y);

        return null;
    }

    public void setShowScrollbar(boolean showScrollbar) {
        if (this.showScrollbar != showScrollbar) {
            if (showScrollbar) {
                listeners.add(scrollbar);
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

    public boolean isHovered() {
        return UIHelper.isWidgetHovered(this);
    }
}
