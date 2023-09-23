package mayo.gui.screens;

import mayo.gui.Screen;
import mayo.gui.widgets.types.Button;
import mayo.render.MatrixStack;
import mayo.render.batch.VertexConsumer;
import mayo.text.Style;
import mayo.text.Text;
import mayo.utils.TextUtils;

public class PauseScreen extends Screen {

    private static final Text PAUSE_TEXT = Text.of("Game Paused...").withStyle(Style.EMPTY.italic(true));

    @Override
    public void init() {
        super.init();

        Button resume = new Button((width - 180) / 2, (height - 20) / 2, 180, 20, Text.of("Resume game"), () -> client.setScreen(null));
        this.addWidget(resume);

        Button menu = new Button(resume.getX(), resume.getY() + resume.getHeight() + 16, 180, 20, Text.of("Main menu"), () -> {
            client.setScreen(new MainMenu());
            client.world = null;
        });
        this.addWidget(menu);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        super.render(matrices, mouseX, mouseY, delta);
        font.render(VertexConsumer.FONT, matrices, width / 2f, 4f, PAUSE_TEXT, TextUtils.Alignment.CENTER);
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
