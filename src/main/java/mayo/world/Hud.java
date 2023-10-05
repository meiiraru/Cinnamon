package mayo.world;

import mayo.Client;
import mayo.gui.widgets.types.ProgressBar;
import mayo.model.GeometryHelper;
import mayo.model.Vertex;
import mayo.render.Font;
import mayo.render.MatrixStack;
import mayo.render.Texture;
import mayo.render.Window;
import mayo.render.batch.VertexConsumer;
import mayo.render.shader.Shader;
import mayo.render.shader.Shaders;
import mayo.text.Style;
import mayo.text.Text;
import mayo.utils.*;
import mayo.world.effects.Effect;
import mayo.world.entity.living.Inventory;
import mayo.world.entity.living.Player;
import mayo.world.items.CooldownItem;
import mayo.world.items.Item;
import mayo.world.items.ItemRenderContext;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

import static org.lwjgl.opengl.GL11.*;

public class Hud {

    private final Texture CROSSHAIR = Texture.of(new Resource("textures/gui/crosshair.png"));
    private final Texture HOTBAR = Texture.of(new Resource("textures/gui/hotbar.png"));
    private final Texture VIGNETTE = Texture.of(new Resource("textures/gui/vignette.png"));
    private final Texture HIT_DIRECTION = Texture.of(new Resource("textures/gui/hit_direction.png"));

    private ProgressBar health, itemCooldown;

    public void init() {
        health = new ProgressBar(1f, 0, 0, 60, 8);
        health.setColor(Colors.RED);

        itemCooldown = new ProgressBar(0f, 0, 0, 60, 8);
    }

    public void tick() {}

    public void render(MatrixStack matrices, float delta) {
        Client c = Client.getInstance();
        int w = c.window.scaledWidth;
        int h = c.window.scaledHeight;

        //render debug text
        Style style = Style.EMPTY.shadow(true).shadowColor(Colors.DARK_GRAY);
        c.font.render(VertexConsumer.FONT, matrices, 4, 4, Text.of(c.fps + " fps").withStyle(style));
        if (c.world.isDebugRendering()) {
            c.font.render(VertexConsumer.FONT, matrices, 4, 4 + c.font.lineHeight * 2, Text.of(debugLeftText()).withStyle(style));
            c.font.render(VertexConsumer.FONT, matrices, w - 4, 4, Text.of(debugRightText()).withStyle(style), TextUtils.Alignment.RIGHT);
        }

        //draw player stats
        drawPlayerStats(matrices, c.world.player, delta);

        //draw crosshair
        VertexConsumer.GUI.consume(GeometryHelper.quad(matrices, (int) (w / 2f - 8), (int) (h / 2f - 8), 16, 16), CROSSHAIR.getID());
    }

    private void drawPlayerStats(MatrixStack matrices, Player player, float delta) {
        if (player == null)
            return;

        //draw hp and other stuff
        drawHealth(matrices, player, delta);

        //draw item stats
        drawItemStats(matrices, player.getHoldingItem(), delta);

        //effects
        drawEffects(matrices, player, delta);

        //hotbar
        drawHotbar(matrices, player, delta);

        //hit direction
        drawHitDirection(matrices, player, delta);
    }

    private void drawHealth(MatrixStack matrices, Player player, float delta) {
        Window window = Client.getInstance().window;
        Font font = Client.getInstance().font;

        //health text
        Text text = Text.of(player.getHealth() + " ").withStyle(Style.EMPTY.outlined(true))
                .append(Text.of("\u2795").withStyle(Style.EMPTY.color(Colors.RED)));

        //transform matrices
        matrices.push();
        matrices.translate(12, window.scaledHeight - TextUtils.getHeight(text, font) - 12, 0f);
        matrices.push();

        matrices.rotate(Rotation.Y.rotationDeg(-20f));
        matrices.rotate(Rotation.Z.rotationDeg(-10f));

        //draw text
        font.render(VertexConsumer.FONT, matrices, 0f, 0f, text);

        //health progress bar
        float hp = player.getHealthProgress();
        health.setProgress(hp);
        health.setY(TextUtils.getHeight(text, font));
        health.render(matrices, 0, 0, delta);

        matrices.pop();
        matrices.pop();

        //vignette
        Vertex[] vertices = GeometryHelper.quad(
                matrices,
                0, 0,
                window.scaledWidth, window.scaledHeight
        );

        float vignette = 1 - Math.min(hp, 0.3f) / 0.3f;
        int color = ((int) (vignette * 0xFF) << 24) + 0xFF0000;

        for (Vertex vertex : vertices) {
            vertex.color(color);
            vertex.getPosition().z = -999;
        }

        VertexConsumer.GUI.consume(vertices, VIGNETTE.getID());
    }

    private void drawItemStats(MatrixStack matrices, Item item, float delta) {
        if (item == null)
            return;

        Window window = Client.getInstance().window;
        Font font = Client.getInstance().font;
        boolean onCooldown = item instanceof CooldownItem ci && ci.isOnCooldown();

        //item name
        Text text = Text.of(item.getId()).withStyle(Style.EMPTY.outlined(true));
        int y = TextUtils.getHeight(text, font);

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
        font.render(VertexConsumer.FONT, matrices, 0f, 0f, text, TextUtils.Alignment.RIGHT);

        //draw progressbar
        if (onCooldown) {
            itemCooldown.setProgress(((CooldownItem) item).getCooldownProgress());
            itemCooldown.setPos(-itemCooldown.getWidth(), y);
            itemCooldown.render(matrices, 0, 0, delta);
        }

        matrices.pop();
        matrices.pop();
    }

    private void drawEffects(MatrixStack matrices, Player player, float delta) {
        //transform matrices
        matrices.push();
        matrices.translate(Client.getInstance().window.scaledWidth - 12, 12, 0f);

        Font font = Client.getInstance().font;
        Text text = Text.empty().withStyle(Style.EMPTY.outlined(true));

        for (Effect effect : player.getActiveEffects()) {
            //name
            text.append(effect.getType().name());

            //amplitude
            int amplitude = effect.getAmplitude();
            if (amplitude != 1)
                text.append(" " + amplitude);

            //divider
            text.append(" - ");

            //remaining time
            text.append(Text.of(((effect.getRemainingTime()) / 20) + "").withStyle(Style.EMPTY.color(Colors.RED)));

            text.append("\n");
        }

        //render
        if (!text.asString().equals("\n"))
            font.render(VertexConsumer.FONT, matrices, 0f, 0f, text, TextUtils.Alignment.RIGHT);

        matrices.pop();
    }

    private void drawHotbar(MatrixStack matrices, Player player, float delta) {
        //set shader
        Shader sh = Shaders.MODEL.getShader().use();
        sh.setProjectionMatrix(Client.getInstance().camera.getOrthographicMatrix());
        sh.setViewMatrix(new Matrix4f());
        sh.setColor(-1);

        //prepare variables
        Window window = Client.getInstance().window;
        Inventory inventory = player.getInventory();
        int count = inventory.getSize();
        int selected = inventory.getSelectedIndex();

        float x = (window.scaledWidth - 16 * count) / 2f;
        float y = window.scaledHeight - 16 - 4f;

        //render items
        for (int i = 0; i < count; i++, x += 16) {
            //render slot
            VertexConsumer.GUI.consume(GeometryHelper.quad(
                            matrices,
                            x, y, 16, 16,
                            i == selected ? 16f : 0f, 0f,
                            16, 16,
                            32, 16
                    ), HOTBAR.getID()
            );

            //render item model
            Item item = inventory.getItem(i);
            if (item != null) {
                matrices.push();
                matrices.translate(x + 8, y + 8, 500);
                matrices.rotate(Rotation.Y.rotationDeg(-90));
                matrices.rotate(Rotation.X.rotationDeg(35));
                matrices.scale(-8);

                item.render(ItemRenderContext.HUD, matrices, delta);

                matrices.pop();
            }
        }
    }

    private void drawHitDirection(MatrixStack matrices, Player player, float delta) {
        int ticks = player.getDamageSourceTicks();
        if (ticks == 0)
            return;

        Float angle = player.getDamageAngle();
        if (angle == null)
            return;

        //window
        Client c = Client.getInstance();
        int w = c.window.scaledWidth;
        int h = c.window.scaledHeight;

        //rotate
        matrices.push();
        matrices.translate((int) (w / 2f), (int) (h / 2f), 0f);
        matrices.rotate(Rotation.Z.rotationDeg(angle));

        //draw
        Vertex[] vertices = GeometryHelper.quad(matrices, -16f, -16f, 32, 32);
        int color = ColorUtils.lerpARGBColor(0x00FFFFFF, -1, Math.min(ticks - delta, 5) / 5f);

        for (Vertex vertex : vertices)
            vertex.color(color);

        VertexConsumer.GUI.consume(vertices, HIT_DIRECTION.getID());

        matrices.pop();
    }

    private static String debugLeftText() {
        Client c = Client.getInstance();

        World w = c.world;
        Vector3f epos = w.player.getPos();
        Vector2f erot = w.player.getRot();
        Vector3f emot = w.player.getMotion();
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
                        [world]
                        %s entities %s particles %s terrain

                        [player]
                        xyz %.3f %.3f %.3f
                        pitch %.3f yaw %.3f
                        motion %.3f %.3f %.3f

                        [camera]
                        xyz %.3f %.3f %.3f
                        pitch %.3f yaw %.3f
                        facing %s
                        """,
                w.entityCount(), w.particleCount(), w.terrainCount(),

                epos.x, epos.y, epos.z,
                erot.x, erot.y,
                emot.x, emot.y, emot.z,

                cpos.x, cpos.y, cpos.z,
                crot.x, crot.y,
                face
        );
    }

    private static String debugRightText() {
        Runtime r = Runtime.getRuntime();
        long max = r.maxMemory();
        long total = r.totalMemory();
        long free = r.freeMemory();
        long used = total - free;

        Window w = Client.getInstance().window;

        return String.format("""
                [java]
                version %s
                mem %s%% %s/%s
                allocated %s%% %s

                [renderer]
                %s
                OpenGL %s

                [window]
                %s x %s
                gui scale %s
                """,
                System.getProperty("java.version"),
                used * 100 / max, Meth.prettyByteSize(used), Meth.prettyByteSize(max),
                total * 100 / max, Meth.prettyByteSize(total),

                glGetString(GL_RENDERER),
                glGetString(GL_VERSION),

                w.width, w.height,
                w.guiScale
        );
    }
}
