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
import mayo.render.shader.Shaders;
import mayo.text.Style;
import mayo.text.Text;
import mayo.utils.*;
import mayo.world.collisions.Hit;
import mayo.world.effects.Effect;
import mayo.world.entity.living.Player;
import mayo.world.items.CooldownItem;
import mayo.world.items.Inventory;
import mayo.world.items.Item;
import mayo.world.items.ItemRenderContext;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL14.glBlendFuncSeparate;

public class Hud {

    private final Texture CROSSHAIR = Texture.of(new Resource("textures/gui/hud/crosshair.png"));
    private final Texture HOTBAR = Texture.of(new Resource("textures/gui/hud/hotbar.png"));
    private final Texture VIGNETTE = Texture.of(new Resource("textures/gui/hud/vignette.png"));
    private final Texture HIT_DIRECTION = Texture.of(new Resource("textures/gui/hud/hit_direction.png"));

    private ProgressBar health, itemCooldown;

    public void init() {
        health = new ProgressBar(0, 0, 60, 8, 1f);
        health.setColor(Colors.RED);

        itemCooldown = new ProgressBar(0, 0, 60, 8, 0f);
    }

    public void tick() {}

    public void render(MatrixStack matrices, float delta) {
        Client c = Client.getInstance();

        //render debug text
        Style style = Style.EMPTY.shadow(true).shadowColor(Colors.DARK_GRAY);
        c.font.render(VertexConsumer.FONT, matrices, 4, 4, Text.of(c.fps + " fps").withStyle(style));
        if (c.world.isDebugRendering()) {
            c.font.render(VertexConsumer.FONT, matrices, 4, 4 + c.font.lineHeight * 2, Text.of(debugLeftText()).withStyle(style));
            c.font.render(VertexConsumer.FONT, matrices, c.window.scaledWidth - 4, 4, Text.of(debugRightText()).withStyle(style), Alignment.RIGHT);
        }

        //draw player stats
        drawPlayerStats(matrices, c.world.player, delta);

        //finish rendering
        Matrix4f projMat = c.camera.getOrthographicMatrix();
        Matrix4f viewMat = new Matrix4f();
        VertexConsumer.finishAllBatches(projMat, viewMat);

        //draw crosshair separated
        drawCrosshair(matrices, projMat, viewMat);
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
                    .append(Text.of(item.getCount()).withStyle(style))
                    .append(" / ")
                    .append(Text.of(item.getStackCount()).withStyle(style));
        }

        //transform matrices
        matrices.push();
        matrices.translate(window.scaledWidth - 12, window.scaledHeight - y - 12, 0f);
        matrices.push();
        matrices.rotate(Rotation.Y.rotationDeg(-20f));
        matrices.rotate(Rotation.Z.rotationDeg(10f));

        //draw text
        font.render(VertexConsumer.FONT, matrices, 0f, 0f, text, Alignment.RIGHT);

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
            text.append(Text.of((effect.getRemainingTime()) / 20).withStyle(Style.EMPTY.color(Colors.RED)));

            text.append("\n");
        }

        //render
        if (!text.asString().equals("\n"))
            font.render(VertexConsumer.FONT, matrices, 0f, 0f, text, Alignment.RIGHT);

        matrices.pop();
    }

    private void drawHotbar(MatrixStack matrices, Player player, float delta) {
        //set shader
        Shaders.MODEL.getShader().use().setup(
                Client.getInstance().camera.getOrthographicMatrix(),
                new Matrix4f()
        );

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

    private void drawCrosshair(MatrixStack matrices, Matrix4f projMat, Matrix4f viewMat) {
        Client c = Client.getInstance();
        int w = c.window.scaledWidth;
        int h = c.window.scaledHeight;

        if (c.world.isDebugRendering()) {
            matrices.push();
            matrices.translate(w / 2f, h / 2f, 0);
            matrices.scale(1, 1, -1);

            Vector2f rot = c.camera.getRot();
            matrices.rotate(Rotation.X.rotationDeg(rot.x));
            matrices.rotate(Rotation.Y.rotationDeg(-rot.y));

            float len = 10;
            GeometryHelper.pushCube(VertexConsumer.GUI, matrices, 1, 0, 0, len, 1, 1, 0xFFFF0000);
            GeometryHelper.pushCube(VertexConsumer.GUI, matrices, 0, 0, 0, 1, -len, 1, 0xFF00FF00);
            GeometryHelper.pushCube(VertexConsumer.GUI, matrices, 0, 0, 0, 1, 1, -len, 0xFF0000FF);

            matrices.pop();
        } else {
            glBlendFuncSeparate(GL_ONE_MINUS_DST_COLOR, GL_ONE_MINUS_SRC_COLOR, GL_ONE, GL_ZERO);

            VertexConsumer.GUI.consume(GeometryHelper.quad(matrices, (int) (w / 2f - 8), (int) (h / 2f - 8), 16, 16), CROSSHAIR.getID());
            VertexConsumer.GUI.finishBatch(projMat, viewMat);

            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        }
    }

    private static String debugLeftText() {
        Client c = Client.getInstance();

        int soundCount = c.soundManager.getSoundCount();

        WorldClient w = c.world;
        Vector3f epos = w.player.getPos();
        Vector2f erot = w.player.getRot();
        Vector3f emot = w.player.getMotion();
        Vector3f cpos = c.camera.getPos();
        Vector2f crot = c.camera.getRot();

        Vector3f chunk = new Vector3f(
                (int) Math.floor(epos.x / 32f),
                (int) Math.floor(epos.y / 32f),
                (int) Math.floor(epos.z / 32f)
        );

        String face;
        float yaw = Maths.modulo(crot.y, 360);
        if (yaw >= 45 && yaw < 135) {
            face = "East X+";
        } else if (yaw >= 135 && yaw < 225) {
            face = "South Z+";
        } else if (yaw >= 225 && yaw < 315) {
            face = "West X-";
        } else {
            face = "North Z-";
        }

        String camera;
        camera = switch (w.getCameraMode()) {
            case 0 -> "First Person";
            case 1 -> "Third Person (back)";
            case 2 -> "Third Person (front)";
            default -> "unknown";
        };

        Player p = c.world.player;
        float range = p.getPickRange();
        String entity = getTargetedObjString(p.getLookingEntity(range), range);
        String terrain = getTargetedObjString(p.getLookingTerrain(range), range);

        return String.format("""
                        [world]
                        %s entities %s terrain
                        %s particles %s sounds
                        %s light sources
 
                        [player]
                        xyz %.3f %.3f %.3f
                        pitch %.3f yaw %.3f
                        motion %.3f %.3f %.3f
                        chunk %.0f %.0f %.0f

                        [camera]
                        xyz %.3f %.3f %.3f
                        pitch %.3f yaw %.3f
                        facing %s
                        mode %s

                        [targeted entity]
                        %s

                        [targeted terrain]
                        %s
                        """,
                w.entityCount(), w.terrainCount(),
                w.particleCount(), soundCount,
                w.lightCount(),

                epos.x, epos.y, epos.z,
                erot.x, erot.y,
                emot.x, emot.y, emot.z,
                chunk.x, chunk.y, chunk.z,

                cpos.x, cpos.y, cpos.z,
                crot.x, crot.y,
                face,
                camera,

                entity,
                terrain
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
                used * 100 / max, Maths.prettyByteSize(used), Maths.prettyByteSize(max),
                total * 100 / max, Maths.prettyByteSize(total),

                glGetString(GL_RENDERER),
                glGetString(GL_VERSION),

                w.width, w.height,
                w.guiScale
        );
    }

    private static String getTargetedObjString(Hit<? extends WorldObject> hit, float range) {
        if (hit == null)
            return "---";

        Vector3f pos = hit.obj().getPos();
        Vector3f hPos = hit.pos();
        Vector3f normal = hit.collision().normal();
        float distance = range * hit.collision().near();
        return String.format("""
                pos %.3f %.3f %.3f
                hit pos %.3f %.3f %.3f
                hit normal %.3f %.3f %.3f
                hit distance %.3fm
                type %s
                """,
                pos.x, pos.y, pos.z,
                hPos.x, hPos.y, hPos.z,
                normal.x, normal.y, normal.z,
                distance,
                hit.obj().getType().name()
        );
    }
}
