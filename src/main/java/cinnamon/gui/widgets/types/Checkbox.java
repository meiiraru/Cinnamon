package cinnamon.gui.widgets.types;

import cinnamon.model.GeometryHelper;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.text.Text;
import cinnamon.utils.Alignment;
import cinnamon.utils.TextUtils;
import org.joml.Math;

import java.util.function.Consumer;

public class Checkbox extends Button {

    protected boolean toggled;
    protected int textSpacing = 4;
    private boolean rtl;

    public Checkbox(int x, int y, Text message) {
        super(x, y, 0, 0, message, null);

        //force updates
        setAction(null);
        setMessage(message);
    }

    @Override
    protected void renderBackground(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        VertexConsumer.MAIN.consume(
                GeometryHelper.quad(
                        matrices,
                        rtl ? getX() + getWidth() - 8 : getX(), getCenterY() - 5,
                        8, 9,
                        getState() * 8f, toggled ? 9f : 0f,
                        8, 9,
                        32, 18
                ), getSkin().getResource("checkbox_tex")
        );
    }

    @Override
    protected void renderText(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        float x = rtl ? getX() : getX() + getButtonWidth() + textSpacing;
        Text text = getFormattedMessage();
        text.render(VertexConsumer.MAIN, matrices, x, getCenterY(), Alignment.CENTER_LEFT);
    }

    @Override
    public void setAction(Consumer<Button> action) {
        Consumer<Button> consumer = button -> {
            toggled = !toggled;
            if (action != null)
                action.accept(this);
        };

        super.setAction(consumer);
    }

    @Override
    public void setMessage(Text message) {
        super.setMessage(message);
        updateDimensions();
    }

    @Override
    protected void updateDimensions() {
        this.setDimensions(
                Math.max(getButtonWidth(), getButtonWidth() + (message != null ? textSpacing + TextUtils.getWidth(message) : 0)),
                Math.max(getButtonHeight(), message != null ? TextUtils.getHeight(message) : 0)
        );

        super.updateDimensions();
    }

    public boolean isToggled() {
        return toggled;
    }

    public void setToggled(boolean toggled) {
        this.toggled = toggled;
    }

    protected int getButtonWidth() {
        return 8;
    }

    protected int getButtonHeight() {
        return 9;
    }

    public int getTextSpacing() {
        return textSpacing;
    }

    public void setTextSpacing(int textSpacing) {
        this.textSpacing = textSpacing;
        updateDimensions();
    }

    public void setRightToLeft(boolean bool) {
        this.rtl = bool;
    }

    public boolean isRightToLeft() {
        return rtl;
    }
}
