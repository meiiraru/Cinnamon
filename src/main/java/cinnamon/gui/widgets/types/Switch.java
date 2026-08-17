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
                        isRightToLeft() ? getX() + getWidth() - 24 : getX(), getCenterY() - 6,
                        24, 12,
                        toggled ? 24f : 0f, getState() * 12f,
                        24, 12,
                        48, 48
                ), getSkin().getResource("switch_tex")
        );
    }

    @Override
    protected int getButtonWidth() {
        return 24;
    }

    @Override
    protected int getButtonHeight() {
        return 12;
    }
}
