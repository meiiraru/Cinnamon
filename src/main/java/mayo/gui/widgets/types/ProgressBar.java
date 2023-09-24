package mayo.gui.widgets.types;

import mayo.gui.widgets.Widget;
import mayo.model.GeometryHelper;
import mayo.model.Vertex;
import mayo.render.MatrixStack;
import mayo.render.Texture;
import mayo.render.batch.VertexConsumer;
import mayo.utils.Colors;
import mayo.utils.Meth;
import mayo.utils.Resource;
import mayo.utils.UIHelper;

public class ProgressBar extends Widget {

    private static final Texture TEXTURE = Texture.of(new Resource("textures/gui/progress_bar.png"));

    private float progress, newProgress;
    private int color = Colors.WHITE.rgba;

    public ProgressBar(float initialValue, int x, int y, int width, int height) {
        super(x, y, width, height);
        setProgress(initialValue, true);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        //draw background
        UIHelper.nineQuad(
                VertexConsumer.GUI, matrices, TEXTURE.getID(),
                getX(), getY(),
                getWidth(), getHeight(),
                0f, 0f,
                16, 16,
                32, 16
        );

        //smooth out progress changes
        this.progress = Meth.lerp(this.progress, this.newProgress, delta);

        //draw progress
        Vertex[] vertices = GeometryHelper.quad(
                matrices,
                getX() + 1, getY() + 1,
                (getWidth() - 2) * progress, getHeight() - 2,
                16f, 0f,
                16 * progress, 16,
                32, 16
        );
        for (Vertex vertex : vertices) {
            vertex.color(color);
            vertex.getPosition().z += 0.001f;
        }

        VertexConsumer.GUI.consume(vertices, TEXTURE.getID());
    }

    public void setProgress(float progress) {
        setProgress(progress, false);
    }

    public void setProgress(float progress, boolean force) {
        this.newProgress = progress;
        if (force) this.progress = progress;
    }

    public float getProgress() {
        return newProgress;
    }

    public boolean isCompleted() {
        return this.newProgress >= 1f;
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
}
