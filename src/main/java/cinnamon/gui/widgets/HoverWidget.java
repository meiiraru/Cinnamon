package cinnamon.gui.widgets;

import cinnamon.math.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.text.Text;
import cinnamon.utils.UIHelper;

public abstract class HoverWidget extends SelectableWidget {

    protected boolean renderBackground = true;
    protected boolean forceBackground = false;

    public HoverWidget(int x, int y, int width, int height) {
        super(x, y, width, height);
        this.setSelectable(false);
    }

    @Override
    public void renderWidget(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        if (renderBackground() && (forceBackground() || (isSelectable() && isHoveredOrFocused())))
            renderHover(matrices, mouseX, mouseY, delta);
    }

    protected void renderHover(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        float x = this instanceof AlignedWidget aw ? aw.getAlignedX() : getX();
        float y = this instanceof AlignedWidget aw ? aw.getAlignedY() : getY();
        UIHelper.nineQuad(
                VertexConsumer.MAIN, matrices, getSkin().getResource("selected_tex"),
                x - 1, y - 1,
                getWidth() + 2, getHeight() + 2,
                0f, 0f,
                15, 15,
                15, 15
        );
    }

    protected void updateSelectable() {
        setSelectable(getTooltip() != null || getPopup() != null);
    }

    public boolean renderBackground() {
        return renderBackground;
    }

    public void setRenderBackground(boolean renderBackground) {
        this.renderBackground = renderBackground;
    }

    public boolean forceBackground() {
        return forceBackground;
    }

    public void setForceBackground(boolean forceBackground) {
        this.forceBackground = forceBackground;
    }

    @Override
    public void setTooltip(Text tooltip) {
        super.setTooltip(tooltip);
        updateSelectable();
    }

    @Override
    public void setPopup(PopupWidget popup) {
        super.setPopup(popup);
        updateSelectable();
    }
}
