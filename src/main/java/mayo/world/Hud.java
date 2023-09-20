package mayo.world;

import mayo.Client;
import mayo.world.items.Item;
import mayo.model.GeometryHelper;
import mayo.render.Font;
import mayo.render.MatrixStack;
import mayo.render.Texture;
import mayo.render.Window;
import mayo.render.batch.VertexConsumer;
import mayo.text.Style;
import mayo.text.Text;
import mayo.utils.Colors;
import mayo.utils.Resource;
import mayo.utils.Rotation;
import mayo.utils.TextUtils;
import mayo.world.entity.Player;
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
        c.font.render(VertexConsumer.FONT, matrices.peek(), 4, 4, Text.of(getDebugText()).withStyle(Style.EMPTY.shadow(true).shadowColor(Colors.DARK_GRAY)));

        //draw crosshair
        VertexConsumer.GUI.consume(GeometryHelper.quad(matrices.peek(), (int) (w / 2f - 8), (int) (h / 2f - 8), 16, 16), CROSSHAIR.getID());

        //draw player stats
        drawPlayerStats(matrices, c.world.player, delta);
    }

    private void drawPlayerStats(MatrixStack matrices, Player player, float delta) {
        if (player == null)
            return;

        //draw hp and other stuff
        drawHealth(matrices, player, delta);

        //draw item stats
        drawItemStats(matrices, player.getHoldingItem(), delta);
    }

    private void drawHealth(MatrixStack matrices, Player player, float delta) {
        Window window = Client.getInstance().window;
        Font font = Client.getInstance().font;

        //item name
        Text text = Text.of(player.getHealth() + " ").withStyle(Style.EMPTY.outlined(true))
                .append(Text.of("\u2795").withStyle(Style.EMPTY.color(Colors.RED)));

        //draw text
        matrices.push();
        matrices.translate(12, window.scaledHeight - font.height(text) - 12, 0f);
        matrices.push();
        matrices.rotate(Rotation.Y.rotationDeg(-20f));
        matrices.rotate(Rotation.Z.rotationDeg(-10f));
        font.render(VertexConsumer.FONT, matrices.peek(), 0f, 0f, text);
        matrices.pop();
        matrices.pop();
    }

    private void drawItemStats(MatrixStack matrices, Item item, float delta) {
        if (item == null)
            return;

        Window window = Client.getInstance().window;
        Font font = Client.getInstance().font;

        //item name
        Text text = Text.of(item.getId()).withStyle(Style.EMPTY.outlined(true));

        //item count
        Style style = Style.EMPTY.color(Colors.RED);
        text
                .append("\n")
                .append(Text.of( item.getCount() + "").withStyle(style))
                .append(" / ")
                .append(Text.of( item.getStackCount() + "").withStyle(style));

        //draw texts
        matrices.push();
        matrices.translate(window.scaledWidth - 12, window.scaledHeight - font.height(text) - 12, 0f);
        matrices.push();
        matrices.rotate(Rotation.Y.rotationDeg(-20f));
        matrices.rotate(Rotation.Z.rotationDeg(10f));
        font.render(VertexConsumer.FONT, matrices.peek(), 0f, 0f, text, TextUtils.Alignment.RIGHT);
        matrices.pop();
        matrices.pop();
    }

    private static String getDebugText() {
        Client c = Client.getInstance();
        Vector3f cpos = c.camera.getPos();
        Vector2f crot = c.camera.getRot();
        Vector3f epos = c.world.player.getPos(1f);
        Vector2f erot = c.world.player.getRot(1f);

        return String.format("""
                        %s fps

                        [world]
                        %s entities %s objects

                        [entity]
                        xyz %.3f %.3f %.3f
                        pitch %.3f yaw %.3f

                        [camera]
                        xyz %.3f %.3f %.3f
                        pitch %.3f yaw %.3f
                        """,
                c.fps,

                c.world.entityCount(), c.world.objectCount(),

                epos.x, epos.y, epos.z,
                erot.y, erot.x,

                cpos.x, cpos.y, cpos.z,
                crot.x, crot.y
        );
    }
}
