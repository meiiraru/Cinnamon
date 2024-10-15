package cinnamon.gui.widgets.types;

import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.utils.Resource;
import cinnamon.utils.UIHelper;

public class Scrollbar extends Slider {

    private static final Resource TEXTURE = new Resource("textures/gui/widgets/scrollbar.png");

    public Scrollbar(int x, int y, int size) {
        super(x, y, size, 8);
        this.setVertical(true);
        this.showValueTooltip(false);
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

        //background
        UIHelper.nineQuad(VertexConsumer.GUI, matrices, TEXTURE,
                x, y, w, h,
                s, 0f,
                8, 8,
                24, 16
        );

        float anim = getAnimationValue();
        if (isVertical()) {
            UIHelper.verticalQuad(VertexConsumer.GUI, matrices, TEXTURE,
                    x, y + Math.round((h - handleSize) * anim),
                    8, handleSize,
                    s, 8f,
                    8, 8,
                    24, 16
            );
        } else {
            UIHelper.horizontalQuad(VertexConsumer.GUI, matrices, TEXTURE,
                    x + Math.round((w - handleSize) * anim), y,
                    handleSize, 8,
                    s, 8f,
                    8, 8,
                    24, 16
            );
        }
    }

    public void setHandlePercentage(float handleSize) {
        int size = isVertical() ? getHeight() : getWidth();
        this.handleSize = Math.max(8, Math.round(size * Math.min(handleSize, 1f)));
    }

    @Override
    protected boolean updateValueOnClick() {
        return !isHandleHovered();
    }
}
