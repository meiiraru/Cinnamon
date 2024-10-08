package cinnamon.world;

import cinnamon.Client;
import cinnamon.gui.widgets.types.ProgressBar;
import cinnamon.model.GeometryHelper;
import cinnamon.model.Vertex;
import cinnamon.registry.MaterialRegistry;
import cinnamon.registry.TerrainRegistry;
import cinnamon.render.Font;
import cinnamon.render.MatrixStack;
import cinnamon.render.Window;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.render.shader.PostProcess;
import cinnamon.render.shader.Shaders;
import cinnamon.text.Style;
import cinnamon.text.Text;
import cinnamon.utils.*;
import cinnamon.world.collisions.Hit;
import cinnamon.world.effects.Effect;
import cinnamon.world.entity.living.Player;
import cinnamon.world.items.CooldownItem;
import cinnamon.world.items.Inventory;
import cinnamon.world.items.Item;
import cinnamon.world.items.ItemRenderContext;
import cinnamon.world.terrain.Terrain;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL14.glBlendFuncSeparate;

public class Hud {

    public static final Resource
            CROSSHAIR = new Resource("textures/gui/hud/crosshair.png"),
            HOTBAR = new Resource("textures/gui/hud/hotbar.png"),
            VIGNETTE = new Resource("textures/gui/hud/vignette.png"),
            HIT_DIRECTION = new Resource("textures/gui/hud/hit_direction.png"),
            PROGRESS_BAR = new Resource("textures/gui/hud/progress_bar.png");

    private ProgressBar health, itemCooldown;

    public void init() {
        health = new ProgressBar(0, 0, 60, 8, 1f);
        health.setColor(Colors.RED);
        health.setTexture(PROGRESS_BAR);

        itemCooldown = new ProgressBar(0, 0, 60, 8, 0f);
        itemCooldown.setColor(Colors.WHITE);
        itemCooldown.setTexture(PROGRESS_BAR);
    }

    public void tick() {}

    public void render(MatrixStack matrices, float delta) {
        Client c = Client.getInstance();

        //render debug text
        if (c.world.isDebugRendering()) {
            Style style = Style.EMPTY.background(true);
            c.font.render(VertexConsumer.FONT, matrices, 4, 4, TextUtils.parseColorFormatting(Text.of(debugLeftText())).withStyle(style));
            c.font.render(VertexConsumer.FONT, matrices, c.window.scaledWidth - 4, 4, TextUtils.parseColorFormatting(Text.of(debugRightText())).withStyle(style), Alignment.RIGHT);
        } else {
            Style style = Style.EMPTY.shadow(true);
            c.font.render(VertexConsumer.FONT, matrices, 4, 4, Text.of(c.fps + " fps @ " + c.ms + " ms").withStyle(style));
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

        //hotbar
        drawHotbar(matrices, player, delta);

        //hit direction
        drawHitDirection(matrices, player, delta);

        if (!Client.getInstance().world.isDebugRendering()) {
            //draw hp and other stuff
            drawHealth(matrices, player, delta);

            //draw item stats
            drawItemStats(matrices, player.getHoldingItem(), delta);

            //effects
            drawEffects(matrices, player, delta);

            //selected terrain
            drawSelectedTerrain(matrices, delta);
        }
    }

    private void drawHealth(MatrixStack matrices, Player player, float delta) {
        Window window = Client.getInstance().window;
        Font font = Client.getInstance().font;

        //health text
        Text text = Text.of(player.getHealth() + " ").withStyle(Style.EMPTY.outlined(true))
                .append(Text.of("\u2764").withStyle(Style.EMPTY.color(Colors.RED)));

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

        VertexConsumer.GUI.consume(vertices, VIGNETTE);
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
            itemCooldown.setProgressWithoutLerp(((CooldownItem) item).getCooldownProgress());
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
                    ), HOTBAR
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
        matrices.translate(Math.round(w / 2f), Math.round(h / 2f), 0f);
        matrices.rotate(Rotation.Z.rotationDeg(angle));

        //draw
        Vertex[] vertices = GeometryHelper.quad(matrices, -16f, -16f, 32, 32);
        int color = ColorUtils.lerpARGBColor(0x00FFFFFF, 0xFFFFFFFF, Math.min(ticks - delta, 5) / 5f);

        for (Vertex vertex : vertices)
            vertex.color(color);

        VertexConsumer.GUI.consume(vertices, HIT_DIRECTION);

        matrices.pop();
    }

    private void drawSelectedTerrain(MatrixStack matrices, float delta) {
        Client c = Client.getInstance();
        WorldClient w = c.world;
        int t = w.getSelectedTerrain();
        int m = w.getSelectedMaterial();

        TerrainRegistry registry = TerrainRegistry.values()[t];
        Terrain terr = registry.getFactory().get();
        MaterialRegistry material = MaterialRegistry.values()[m];
        terr.setMaterial(material);

        Vector3f bounds = terr.getAABB().getDimensions();
        Vector3f center = terr.getAABB().getCenter();
        float s = 16f / bounds.y;

        matrices.push();
        Window ww = c.window;

        //translate to the top center of the screen
        matrices.translate(ww.scaledWidth * 0.5f, 4 + bounds.y * s * 0.5f, 0);
        matrices.scale(s, -s, s);

        //apply rotation for a better view angle of the model
        matrices.rotate(Rotation.X.rotationDeg(20));
        matrices.rotate(Rotation.Y.rotationDeg(-c.ticks - delta));

        //offset to center of the model
        matrices.translate(-center.x, -center.y, -center.z);

        //render terrain
        terr.render(matrices, delta);
        matrices.pop();

        //render name
        String str = (material.name() + " " + registry.name()).replaceAll("_", " ");
        c.font.render(VertexConsumer.FONT, matrices, ww.scaledWidth * 0.5f, 16 + 4 + 4, Text.of(str).withStyle(Style.EMPTY.shadow(true)), Alignment.CENTER);
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
            VertexConsumer.GUI.consume(GeometryHelper.cube(matrices, 1, 0, 0, len, 1, 1, 0xFFFF0000));
            VertexConsumer.GUI.consume(GeometryHelper.cube(matrices, 0, 0, 0, 1, -len, 1, 0xFF00FF00));
            VertexConsumer.GUI.consume(GeometryHelper.cube(matrices, 0, 0, 0, 1, 1, -len, 0xFF0000FF));

            matrices.pop();
        } else {
            glBlendFuncSeparate(GL_ONE_MINUS_DST_COLOR, GL_ONE_MINUS_SRC_COLOR, GL_ONE, GL_ZERO);

            VertexConsumer.GUI.consume(GeometryHelper.quad(matrices, Math.round(w / 2f - 8), Math.round(h / 2f - 8), 16, 16), CROSSHAIR);
            VertexConsumer.GUI.finishBatch(projMat, viewMat);

            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        }
    }

    private static String debugLeftText() {
        Client c = Client.getInstance();

        int soundCount = c.soundManager.getSoundCount();

        WorldClient w = c.world;
        Player p = w.player;

        Vector3f epos = p.getPos();
        Vector2f erot = p.getRot();
        Vector3f emot = p.getMotion();
        Vector3f cpos = c.camera.getPos();
        Vector2f crot = c.camera.getRot();

        Vector3f chunk = new Vector3f(w.getChunkGridPos(epos));

        String face = Direction.fromRotation(crot.y).name;

        String camera;
        camera = switch (w.getCameraMode()) {
            case 0 -> "First Person";
            case 1 -> "Third Person (back)";
            case 2 -> "Third Person (front)";
            default -> "unknown";
        };

        float range = p.getPickRange();
        String entity = getTargetedObjString(p.getLookingEntity(range), range);
        String terrain = getTargetedObjString(p.getLookingTerrain(range), range);

        return String.format("""
                        &e%s&r fps @ &e%s&r ms

                        [&bworld&r]
                         &e%s&r/&e%s&r entities &e%s&r/&e%s&r particles
                         &e%s&r/&e%s&r chunks &e%s&r terrain
                         &e%s&r light sources
                         &e%s&r sounds
                         time &e%s&r
 
                        [&bplayer&r]
                         xyz &c%.3f &a%.3f &b%.3f&r
                         pitch &e%.3f&r yaw &e%.3f&r
                         motion &c%.3f &a%.3f &b%.3f&r
                         chunk &c%.0f &a%.0f &b%.0f&r

                        [&bcamera&r]
                         xyz &c%.3f &a%.3f &b%.3f&r
                         pitch &e%.3f&r yaw &e%.3f&r
                         facing &e%s&r
                         mode &e%s&r

                        [&btargeted entity&r]
                        %s

                        [&btargeted terrain&r]
                        %s
                        """,
                c.fps, c.ms,

                w.getRenderedEntities(), w.entityCount(), w.getRenderedParticles(), w.particleCount(),
                w.getRenderedChunks(), w.chunkCount(), w.getRenderedTerrain(),
                w.lightCount(),
                soundCount,
                w.getTime(),

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
        PostProcess post = Client.getInstance().world.getActivePostProcess();

        return String.format("""
                [&bjava&r]
                version &e%s&r\s
                mem &e%s&r%% &e%s&r/&e%s&r\s
                allocated &e%s&r%% &e%s&r\s

                [&brenderer&r]
                %s\s
                %s\s
                OpenGL &e%s&r\s

                [&bwindow&r]
                &e%s&r x &e%s&r\s
                gui scale &e%s&r\s

                [&bpost process&r]
                %s\s
                """,
                System.getProperty("java.version"),
                used * 100 / max, Maths.prettyByteSize(used), Maths.prettyByteSize(max),
                total * 100 / max, Maths.prettyByteSize(total),

                System.getProperty("os.name"),
                glGetString(GL_RENDERER),
                glGetString(GL_VERSION),

                w.width, w.height,
                w.guiScale,

                post == null ? "none" : "&e" + post.name() + "&r"
        );
    }

    private static String getTargetedObjString(Hit<? extends WorldObject> hit, float range) {
        if (hit == null)
            return " ---";

        Vector3f pos = hit.obj().getPos();
        Vector3f hPos = hit.pos();
        Vector3f normal = hit.collision().normal();
        float distance = range * hit.collision().near();
        return String.format("""
                 pos &c%.3f &a%.3f &b%.3f&r
                 hit pos &c%.3f &a%.3f &b%.3f&r
                 hit normal &c%.3f &a%.3f &b%.3f&r
                 hit distance &e%.3fm&r
                 type &e%s&r
                """,
                pos.x, pos.y, pos.z,
                hPos.x, hPos.y, hPos.z,
                normal.x, normal.y, normal.z,
                distance,
                hit.obj().getType().name()
        );
    }
}
