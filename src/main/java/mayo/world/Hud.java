package mayo.world;

import mayo.Client;
import mayo.gui.widgets.types.ProgressBar;
import mayo.model.GeometryHelper;
import mayo.render.Font;
import mayo.render.MatrixStack;
import mayo.render.Texture;
import mayo.render.Window;
import mayo.render.batch.VertexConsumer;
import mayo.text.Style;
import mayo.text.Text;
import mayo.utils.*;
import mayo.world.entity.Player;
import mayo.world.items.CooldownItem;
import mayo.world.items.Item;
import org.joml.Vector2f;
import org.joml.Vector3f;

public class Hud {

    private final Texture CROSSHAIR = new Texture(new Resource("textures/crosshair.png"));

    private ProgressBar health, itemCooldown;

    public void init() {
        health = new ProgressBar(1f, 0, 0, 60, 8);
        health.setColor(Colors.RED);

        itemCooldown = new ProgressBar(1f, 0, 0, 60, 8);
    }

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

        //health text
        Text text = Text.of(player.getHealth() + " ").withStyle(Style.EMPTY.outlined(true))
                .append(Text.of("\u2795").withStyle(Style.EMPTY.color(Colors.RED)));

        //transform matrices
        matrices.push();
        matrices.translate(12, window.scaledHeight - font.height(text) - 12, 0f);
        matrices.push();

        matrices.rotate(Rotation.Y.rotationDeg(-20f));
        matrices.rotate(Rotation.Z.rotationDeg(-10f));

        //draw text
        font.render(VertexConsumer.FONT, matrices.peek(), 0f, 0f, text);

        //health progress bar
        health.setProgress(player.getHealthProgress());
        health.setY((int) font.height(text));
        health.render(matrices, 0, 0, delta);

        matrices.pop();
        matrices.pop();
    }

    private void drawItemStats(MatrixStack matrices, Item item, float delta) {
        if (item == null)
            return;

        Window window = Client.getInstance().window;
        Font font = Client.getInstance().font;
        boolean onCooldown = item instanceof CooldownItem ci && ci.isOnCooldown();

        //item name
        Text text = Text.of(item.getId()).withStyle(Style.EMPTY.outlined(true));
        float y = font.height(text);

        //item count
        if (!onCooldown) {
            Style style = Style.EMPTY.color(Colors.RED);
            text
                    .append("\n")
                    .append(Text.of(item.getCount() + "").withStyle(style))
                    .append(" / ")
                    .append(Text.of(item.getStackCount() + "").withStyle(style));
        }

        //transform matrices
        matrices.push();
        matrices.translate(window.scaledWidth - 12, window.scaledHeight - y - 12, 0f);
        matrices.push();
        matrices.rotate(Rotation.Y.rotationDeg(-20f));
        matrices.rotate(Rotation.Z.rotationDeg(10f));

        //draw text
        font.render(VertexConsumer.FONT, matrices.peek(), 0f, 0f, text, TextUtils.Alignment.RIGHT);

        //draw progressbar
        if (onCooldown) {
            itemCooldown.setProgress(((CooldownItem) item).getCooldownProgress());
            itemCooldown.setPos(-itemCooldown.getWidth(), (int) y);
            itemCooldown.render(matrices, 0, 0, delta);
        }

        matrices.pop();
        matrices.pop();
    }

    private static String getDebugText() {
        Client c = Client.getInstance();

        if (!c.world.isDebugRendering())
            return c.fps + " fps";

        Vector3f epos = c.world.player.getPos(1f);
        Vector2f erot = c.world.player.getRot(1f);
        Vector3f cpos = c.camera.getPos();
        Vector2f crot = c.camera.getRot();

        String face;
        float yaw = Meth.modulo(crot.y, 360);
        if (yaw >= 45 && yaw < 135) {
            face = "East X+";
        } else if (yaw >= 135 && yaw < 225) {
            face = "South Z+";
        } else if (yaw >= 225 && yaw < 315) {
            face = "West X-";
        } else {
            face = "North Z-";
        }

        return String.format("""
                        %s fps

                        [world]
                        %s entities %s objects

                        [player]
                        xyz %.3f %.3f %.3f
                        pitch %.3f yaw %.3f

                        [camera]
                        xyz %.3f %.3f %.3f
                        pitch %.3f yaw %.3f
                        facing %s
                        """,
                c.fps,

                c.world.entityCount(), c.world.objectCount(),

                epos.x, epos.y, epos.z,
                erot.x, erot.y,

                cpos.x, cpos.y, cpos.z,
                crot.x, crot.y,
                face
        );
    }
}
