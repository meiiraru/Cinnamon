package cinnamon.gui.widgets.types;

import cinnamon.gui.widgets.Widget;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.utils.Colors;
import cinnamon.utils.Resource;
import cinnamon.utils.UIHelper;
import org.joml.Math;

public class ProgressBar extends Widget {

    protected float progress;
    protected float animationValue;
    protected Integer color;

    public ProgressBar(int x, int y, int width, int height, float initialValue) {
        super(x, y, width, height);
        this.progress = initialValue;
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        float d = UIHelper.tickDelta(0.4f);
        animationValue = Math.lerp(animationValue, getProgress(), d);
        Resource tex = getStyle().getResource("progress_bar_tex");

        //draw background
        UIHelper.nineQuad(
                VertexConsumer.MAIN, matrices, tex,
                getX(), getY(),
                getWidth(), getHeight(),
                0f, 0f,
                16, 16,
                32, 16
        );

        //draw progress
        matrices.pushMatrix();
        matrices.translate(0, 0, UIHelper.getDepthOffset());
        UIHelper.nineQuad(
                VertexConsumer.MAIN, matrices, tex,
                getX(), getY(),
                Math.round(getWidth() * getAnimationValue()), getHeight(),
                16f, 0f,
                16, 16,
                32, 16,
                color == null ? getStyle().getInt("accent_color") : color
        );
        matrices.popMatrix();
    }

    public void setProgress(float progress) {
        this.progress = progress;
    }

    public void setProgressWithoutLerp(float progress) {
        setProgress(progress);
        this.animationValue = progress;
    }

    public float getProgress() {
        return progress;
    }

    public boolean isCompleted() {
        return this.progress >= 1f;
    }

    public void setColor(Colors color) {
        this.setColor(color.argb);
    }

    public void setColor(Integer color) {
        this.color = color;
    }

    public Integer getColor() {
        return color;
    }

    public float getAnimationValue() {
        return animationValue;
    }
}
