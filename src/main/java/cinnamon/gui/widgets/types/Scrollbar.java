package cinnamon.gui.widgets.types;

import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.utils.Resource;
import cinnamon.utils.UIHelper;

public class Scrollbar extends Slider {

    public Scrollbar(int x, int y, int size) {
        super(x, y, size, 8);
        this.setVertical(true);
        this.showValueTooltip(false);
        this.invertY(true);
    }

    @Override
    protected void renderHorizontal(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderScrollbar(matrices, mouseX, mouseY, delta);
    }

    @Override
    protected void renderVertical(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderScrollbar(matrices, mouseX, mouseY, delta);
    }

    protected void renderScrollbar(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();
        int s = 8 * getState();
        Resource tex = getStyle().getResource("scroll_bar_tex");

        //background
        UIHelper.nineQuad(VertexConsumer.MAIN, matrices, tex,
                x, y, w, h,
                s, 0f,
                8, 8,
                24, 16
        );

        matrices.pushMatrix();
        matrices.translate(0, 0, UIHelper.getDepthOffset());

        float anim = getAnimationValue();
        if (isVertical()) {
            UIHelper.verticalQuad(VertexConsumer.MAIN, matrices, tex,
                    x, y + Math.round((h - handleSize) * anim),
                    8, handleSize,
                    s, 8f,
                    8, 8,
                    24, 16
            );
        } else {
            UIHelper.horizontalQuad(VertexConsumer.MAIN, matrices, tex,
                    x + Math.round((w - handleSize) * anim), y,
                    handleSize, 8,
                    s, 8f,
                    8, 8,
                    24, 16
            );
        }

        matrices.popMatrix();
    }

    public void setHandleSize(int handleSize) {
        this.handleSize = Math.max(8, handleSize);
    }

    @Override
    protected boolean updateValueOnClick() {
        return !isHandleHovered();
    }
}
