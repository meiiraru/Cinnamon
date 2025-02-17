package cinnamon.gui.widgets.types;

import cinnamon.model.GeometryHelper;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.text.Text;
import cinnamon.utils.Alignment;
import cinnamon.utils.TextUtils;

import java.util.function.Consumer;

public class Checkbox extends Button {

    protected boolean toggled;
    protected int textSpacing = 4;

    public Checkbox(int x, int y, Text message) {
        super(x, y, 0, 0, message, null);

        //force updates
        setAction(null);
        setMessage(message);
    }

    @Override
    protected void renderBackground(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        VertexConsumer.GUI.consume(
                GeometryHelper.quad(
                        matrices,
                        getX(), getCenterY() - 5,
                        8, 9,
                        getState() * 8f, toggled ? 9f : 0f,
                        8, 9,
                        32, 18
                ), getStyle().checkboxTex
        );
    }

    @Override
    protected void renderText(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        Text text = getFormattedMessage();
        text.render(VertexConsumer.FONT, matrices, getX() + 8 + textSpacing, getCenterY(), Alignment.CENTER_LEFT);
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
                Math.max(8, 8 + textSpacing + TextUtils.getWidth(message)),
                Math.max(8, TextUtils.getHeight(message))
        );

        super.updateDimensions();
    }

    public boolean isToggled() {
        return toggled;
    }

    public void setToggled(boolean toggled) {
        this.toggled = toggled;
    }

    public int getTextSpacing() {
        return textSpacing;
    }

    public void setTextSpacing(int textSpacing) {
        this.textSpacing = textSpacing;
        updateDimensions();
    }
}
