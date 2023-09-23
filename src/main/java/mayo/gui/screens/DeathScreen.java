package mayo.gui.screens;

import mayo.gui.Screen;
import mayo.gui.widgets.types.Button;
import mayo.render.MatrixStack;
import mayo.render.batch.VertexConsumer;
import mayo.text.Style;
import mayo.text.Text;
import mayo.utils.ColorUtils;
import mayo.utils.Colors;
import mayo.utils.TextUtils;

public class DeathScreen extends Screen {

    private static final Text YOU_DIED = Text.of("YOU DIED").withStyle(Style.EMPTY.shadow(true).shadowColor(0xFF440000).underlined(true));

    private Button respawn;

    @Override
    public void init() {
        super.init();

        this.addWidget(respawn = new Button((width - 180) / 2, (height - 20) / 2, 180, 20, Text.of("Respawn"), () -> {
            client.world.respawn();
            client.setScreen(null);
        }));

        Button menu = new Button(respawn.getX(), respawn.getY() + respawn.getHeight() + 16, 180, 20, Text.of("Main menu"), () -> {
            client.setScreen(new MainMenu());
            client.world = null;
        });
        this.addWidget(menu);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        super.render(matrices, mouseX, mouseY, delta);

        matrices.push();

        matrices.translate(width / 2f, respawn.getY() - TextUtils.getHeight(YOU_DIED, font) * 5f - 16f, 0f);
        matrices.scale(5f);

        float dc = (float) (Math.sin((client.ticks + delta) * 0.1f) + 1) * 0.5f;
        int color = ColorUtils.lerpColor(Colors.RED.rgba, 0xFF880000, dc);
        font.render(VertexConsumer.FONT, matrices, 0f, 0f, YOU_DIED.withStyle(Style.EMPTY.color(color)), TextUtils.Alignment.CENTER);

        matrices.pop();
    }

    @Override
    protected void renderBackground(MatrixStack matrices, float delta) {
        renderTranslucentBackground(matrices, delta);
    }
}
