package cinnamon.gui.widgets.types;

import cinnamon.model.GeometryHelper;
import cinnamon.model.Vertex;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.utils.UIHelper;

public class CircularProgressBar extends ProgressBar {

    public CircularProgressBar(int x, int y, float initialValue) {
        super(x, y, 12, 12, initialValue);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        int x = getCenterX();
        int y = getCenterY();
        int r = getWidth() / 2;

        //background
        VertexConsumer.GUI.consume(GeometryHelper.quad(matrices, getX(), getY(), getWidth(), getHeight(), 2, 1), getStyle().circularProgressTex);

        //progress
        matrices.push();
        matrices.translate(0, 0, UIHelper.DEPTH_OFFSET);

        Vertex[] vertices = GeometryHelper.progressSquare(matrices, x, y, r, getProgress(), color == null ? getStyle().accentColor : color);
        for (Vertex vertex : vertices)
            vertex.uv(vertex.getUV().mul(0.5f, 1f).add(0.5f, 0f));
        VertexConsumer.GUI.consume(vertices, getStyle().circularProgressTex);

        matrices.pop();
    }
}
