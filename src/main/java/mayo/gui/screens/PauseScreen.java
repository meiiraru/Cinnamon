package mayo.gui.screens;

import mayo.gui.Screen;
import mayo.gui.widgets.types.Button;
import mayo.model.GeometryHelper;
import mayo.model.Vertex;
import mayo.render.MatrixStack;
import mayo.render.batch.VertexConsumer;
import mayo.text.Text;

public class PauseScreen extends Screen {

    @Override
    public void init() {
        super.init();

        Button resume = new Button((width - 160) / 2, (height - 20) / 2, 160, 20, Text.of("Resume game"), () -> client.setScreen(null));
        this.addWidget(resume);

        Button menu = new Button(resume.getX(), resume.getY() + resume.getHeight() + 16, 160, 20, Text.of("Main menu"), () -> {
            client.setScreen(new MainMenu());
            client.world = null;
        });
        this.addWidget(menu);
    }

    @Override
    public boolean closeOnEsc() {
        return true;
    }

    @Override
    protected void renderBackground(MatrixStack matrices, float delta) {
        Vertex[] vertices = GeometryHelper.quad(matrices.peek(), 0, 0, width, height);
        for (Vertex vertex : vertices)
            vertex.color(0x88 << 24);
        VertexConsumer.GUI.consume(vertices, 0);
    }
}
