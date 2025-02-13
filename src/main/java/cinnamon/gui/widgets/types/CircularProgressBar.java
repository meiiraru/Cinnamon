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

        Vertex[] vertices = GeometryHelper.circle(matrices, x, y, r, 1, 12, 0xFFFFFFFF);

        //background
        for (int i = 0; i < vertices.length - 2; i += 3) {
            vertices[i].uv(0.25f, 1f);
            vertices[i + 1].uv(0.5f, 0f);
            vertices[i + 2].uv(0f, 0f);
        }

        VertexConsumer.GUI.consume(vertices, getStyle().circularProgressTex);

        matrices.push();
        matrices.translate(0, 0, UIHelper.DEPTH_OFFSET);

        vertices = GeometryHelper.circle(matrices, x, y, r, getProgress(), 12, color == null ? getStyle().accentColor : color);
        for (int i = 0; i < vertices.length - 2; i += 3) {
            vertices[i].uv(0.75f, 1f);
            vertices[i + 1].uv(1f, 0f);
            vertices[i + 2].uv(0.5f, 0f);
        }

        VertexConsumer.GUI.consume(vertices, getStyle().circularProgressTex);

        matrices.pop();
    }
}
