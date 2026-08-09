package cinnamon.gui.widgets.types;

import cinnamon.model.GeometryHelper;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.text.Text;

public class Switch extends Checkbox {

    public Switch(int x, int y, Text message) {
        super(x, y, message);
    }

    @Override
    protected void renderBackground(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        VertexConsumer.MAIN.consume(
                GeometryHelper.quad(
                        matrices,
                        isRightToLeft() ? getX() + getWidth() - 16 : getX(), getCenterY() - 4,
                        16, 8,
                        toggled ? 16f : 0f, getState() * 8f,
                        16, 8,
                        32, 32
                ), getSkin().getResource("switch_tex")
        );
    }

    @Override
    protected int getButtonWidth() {
        return 16;
    }

    @Override
    protected int getButtonHeight() {
        return 8;
    }
}
