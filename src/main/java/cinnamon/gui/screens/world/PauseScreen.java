package cinnamon.gui.screens.world;

import cinnamon.gui.Screen;
import cinnamon.gui.widgets.ContainerGrid;
import cinnamon.gui.widgets.types.Button;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.text.Style;
import cinnamon.text.Text;
import cinnamon.utils.Alignment;

public class PauseScreen extends Screen {

    private static final Text PAUSE_TEXT = Text.of("Paused...").withStyle(Style.EMPTY.italic(true));

    @Override
    public void init() {
        super.init();

        ContainerGrid grid = new ContainerGrid(0, 0, 8, 2);

        Button resume = new Button(0, 0, 120, 20, Text.of("Resume"), button -> client.setScreen(null));
        Button menu = new Button(0, 0, 120, 20, Text.of("Main menu"), button -> client.world.close());

        grid.addWidget(resume);
        grid.addWidget(menu);

        grid.setPos((width - grid.getWidth()) / 2, (height - grid.getHeight()) / 2);

        this.addWidget(grid);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        super.render(matrices, mouseX, mouseY, delta);
        font.render(VertexConsumer.FONT, matrices, width / 2f, 4f, PAUSE_TEXT, Alignment.CENTER);
    }

    @Override
    public boolean closeOnEsc() {
        return true;
    }

    @Override
    protected void renderBackground(MatrixStack matrices, float delta) {
        renderTranslucentBackground(matrices, delta);
    }
}
