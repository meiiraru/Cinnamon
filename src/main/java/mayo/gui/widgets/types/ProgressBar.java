package mayo.gui.widgets.types;

import mayo.gui.widgets.Widget;
import mayo.render.MatrixStack;
import mayo.render.Texture;
import mayo.render.batch.VertexConsumer;
import mayo.utils.Colors;
import mayo.utils.Maths;
import mayo.utils.Resource;
import mayo.utils.UIHelper;

public class ProgressBar extends Widget {

    private static final Texture TEXTURE = Texture.of(new Resource("textures/gui/widgets/progress_bar.png"));

    private float progress;
    private float animationValue;
    private int color = Colors.WHITE.rgba;

    public ProgressBar(int x, int y, int width, int height, float initialValue) {
        super(x, y, width, height);
        this.progress = initialValue;
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        float d = UIHelper.tickDelta(0.4f);
        animationValue = Maths.lerp(animationValue, getProgress(), d);

        //draw background
        UIHelper.nineQuad(
                VertexConsumer.GUI, matrices, TEXTURE.getID(),
                getX(), getY(),
                getWidth(), getHeight(),
                0f, 0f,
                16, 16,
                32, 16
        );

        //draw progress
        UIHelper.nineQuad(
                VertexConsumer.GUI, matrices, TEXTURE.getID(),
                getX(), getY(),
                Math.round(getWidth() * getAnimationValue()), getHeight(),
                16f, 0f,
                16, 16,
                32, 16,
                color
        );
    }

    public void setProgress(float progress) {
        this.progress = progress;
    }

    public float getProgress() {
        return progress;
    }

    public boolean isCompleted() {
        return this.progress >= 1f;
    }

    public void setColor(Colors color) {
        this.setColor(color.rgba);
    }

    public void setColor(int color) {
        this.color = color;
    }

    public int getColor() {
        return color;
    }

    public float getAnimationValue() {
        return animationValue;
    }
}
