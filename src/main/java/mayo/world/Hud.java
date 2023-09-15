package mayo.world;

import mayo.Client;
import mayo.render.MatrixStack;
import mayo.render.batch.VertexConsumer;
import mayo.text.Style;
import mayo.text.Text;

public class Hud {

    private Text fps = Text.empty();

    public void tick() {
        this.fps = Text.of(Client.getInstance().fps + " fps").withStyle(Style.EMPTY.shadow(true).shadowColor(0xFFFF7272));
    }

    public void render(MatrixStack matrices, float delta) {
        //render fps
        Client c = Client.getInstance();
        matrices.push();
        matrices.translate(c.window.scaledWidth - c.font.width(fps) - 4f, 4f, 0f);
        c.font.render(VertexConsumer.FONT, matrices.peek(), fps);
        matrices.pop();
    }
}
