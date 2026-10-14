package cinnamon.gui.widgets.types;

import cinnamon.math.Maths;
import cinnamon.model.GeometryHelper;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.utils.Resource;
import cinnamon.utils.UIHelper;
import org.joml.Math;

public class OverlayProgressBar extends ProgressBar {

    protected boolean vertical;
    protected boolean inverted;
    protected Resource icon;

    public OverlayProgressBar(int x, int y, int width, int height, float initialValue) {
        super(x, y, width, height, initialValue);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        if (!isVisible())
            return;

        float d = UIHelper.tickDelta(0.4f);
        animationValue = Math.lerp(animationValue, getProgress(), d);

        if (vertical) renderVertical(matrices, delta);
        else renderHorizontal(matrices, delta);
    }

    protected void renderVertical(MatrixStack matrices, float delta) {
        Resource tex = icon != null ? icon : getSkin().getResource("progress_bar_tex");

        float anim = Maths.clamp(getAnimationValue(), 0f, 1f);
        float y = getY();
        float h = getHeight() * anim;
        float v = 16f;
        float rh = 16f * anim;

        if (inverted) {
            y += getHeight() - h;
            v += 16f - rh;
        }

        //draw background
        VertexConsumer.MAIN.consume(GeometryHelper.quad(
                matrices,
                getX(), getY(),
                getWidth(), getHeight(),
                0f, 0f,
                16, 16,
                16, 32
        ), tex);

        //draw progress
        matrices.pushMatrix();
        matrices.translate(0, 0, UIHelper.getDepthOffset());
        VertexConsumer.MAIN.consume(GeometryHelper.quad(
                matrices,
                getX(), y,
                getWidth(), h,
                0f, v,
                16f, rh,
                16, 32
        ), tex);
        matrices.popMatrix();
    }

    protected void renderHorizontal(MatrixStack matrices, float delta) {
        Resource tex = icon != null ? icon : getSkin().getResource("progress_bar_tex");

        float anim = Maths.clamp(getAnimationValue(), 0f, 1f);
        float x = getX();
        float w = getWidth() * anim;
        float u = 16f;
        float rw = 16f * anim;

        if (inverted) {
            x += getWidth() - w;
            u += 16f - rw;
        }

        //draw background
        VertexConsumer.MAIN.consume(GeometryHelper.quad(
                matrices,
                getX(), getY(),
                getWidth(), getHeight(),
                0f, 0f,
                16, 16,
                32, 16
        ), tex);

        //draw progress
        matrices.pushMatrix();
        matrices.translate(0, 0, UIHelper.getDepthOffset());
        VertexConsumer.MAIN.consume(GeometryHelper.quad(
                matrices,
                x, getY(),
                w, getHeight(),
                u, 0f,
                rw, 16f,
                32, 16
        ), tex);
        matrices.popMatrix();
    }

    public void setIcon(Resource icon) {
        this.icon = icon;
    }

    public Resource getIcon() {
        return icon;
    }

    public void setVertical(boolean vertical) {
        this.vertical = vertical;
    }

    public boolean isVertical() {
        return vertical;
    }

    public void setInverted(boolean inverted) {
        this.inverted = inverted;
    }

    public boolean isInverted() {
        return inverted;
    }
}
