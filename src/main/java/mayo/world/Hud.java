package mayo.world;

import mayo.Client;
import mayo.model.GeometryHelper;
import mayo.render.MatrixStack;
import mayo.render.Texture;
import mayo.render.batch.VertexConsumer;
import mayo.text.Style;
import mayo.text.Text;
import mayo.utils.Colors;
import mayo.utils.Resource;

public class Hud {

    private final Texture CROSSHAIR = new Texture(new Resource("textures/crosshair.png"));

    private Text fps = Text.empty();

    public void tick() {
        this.fps = Text.of(Client.getInstance().fps + " fps").withStyle(Style.EMPTY.shadow(true).shadowColor(Colors.RED));
    }

    public void render(MatrixStack matrices, float delta) {
        Client c = Client.getInstance();
        int w = c.window.scaledWidth;
        int h = c.window.scaledHeight;

        //render fps
        matrices.push();
        matrices.translate(w - c.font.width(fps) - 4f, 4f, 0f);
        c.font.render(VertexConsumer.FONT, matrices.peek(), fps);
        matrices.pop();

        //draw crosshair
        VertexConsumer.GUI.consume(GeometryHelper.quad((int) (w / 2f - 8), (int) (h / 2f - 8), 16, 16), CROSSHAIR.getID());
    }
}
