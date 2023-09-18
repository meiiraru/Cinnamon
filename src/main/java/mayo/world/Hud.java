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
import org.joml.Vector2f;
import org.joml.Vector3f;

public class Hud {

    private final Texture CROSSHAIR = new Texture(new Resource("textures/crosshair.png"));

    public void tick() {}

    public void render(MatrixStack matrices, float delta) {
        Client c = Client.getInstance();
        int w = c.window.scaledWidth;
        int h = c.window.scaledHeight;

        //render debug text
        c.font.render(VertexConsumer.FONT, matrices.peek(), 4, 4, Text.of(getDebugText()).withStyle(Style.EMPTY.shadow(true).shadowColor(Colors.RED)));

        //draw crosshair
        VertexConsumer.GUI.consume(GeometryHelper.quad(matrices.peek(), (int) (w / 2f - 8), (int) (h / 2f - 8), 16, 16), CROSSHAIR.getID());
    }

    private static String getDebugText() {
        Client c = Client.getInstance();
        Vector3f pos = c.camera.getPos();
        Vector2f rot = c.camera.getRot();

        return String.format("""
                        %s fps
                        xyz %.3f %.3f %.3f
                        pitch %.3f
                        yaw %.3f
                        """,
                c.fps,
                pos.x, pos.y, pos.z,
                rot.x,
                rot.y);
    }
}
