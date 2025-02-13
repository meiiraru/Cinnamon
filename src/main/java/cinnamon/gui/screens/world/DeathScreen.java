package cinnamon.gui.screens.world;

import cinnamon.gui.Screen;
import cinnamon.gui.widgets.types.Button;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.text.Style;
import cinnamon.text.Text;
import cinnamon.utils.Alignment;
import cinnamon.utils.ColorUtils;
import cinnamon.utils.Colors;
import cinnamon.utils.TextUtils;

public class DeathScreen extends Screen {

    private static final Text YOU_DIED = Text.of("YOU DIED").withStyle(Style.EMPTY.outlined(true).outlineColor(0xFF440000).underlined(true));

    private Button respawn;

    @Override
    public void init() {
        super.init();

        this.addWidget(respawn = new Button((width - 180) / 2, height / 2 + 8, 180, 20, Text.of("Respawn"), button -> {
            client.world.respawn(false);
            client.setScreen(null);
        }));

        Button menu = new Button(respawn.getX(), respawn.getY() + respawn.getHeight() + 16, 180, 20, Text.of("Main menu"), button -> client.world.close());
        this.addWidget(menu);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        super.render(matrices, mouseX, mouseY, delta);

        matrices.push();

        float textSize = TextUtils.getHeight(YOU_DIED) * 5f;
        matrices.translate(width / 2f, (respawn.getY() - textSize) / 2f, 0f);
        matrices.scale(5f);

        float dc = (float) (Math.sin((client.ticks + delta) * 0.1f) + 1) * 0.5f;
        int color = ColorUtils.lerpARGBColor(Colors.RED.rgba, 0xFF880000, dc);
        YOU_DIED.withStyle(Style.EMPTY.color(color)).render(VertexConsumer.FONT, matrices, 0f, 0f, Alignment.CENTER);

        matrices.pop();
    }

    @Override
    protected void renderBackground(MatrixStack matrices, float delta) {
        renderTranslucentBackground(matrices, delta);
    }
}
