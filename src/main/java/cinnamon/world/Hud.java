package cinnamon.world;

import cinnamon.Client;
import cinnamon.gui.widgets.types.ProgressBar;
import cinnamon.model.GeometryHelper;
import cinnamon.model.Vertex;
import cinnamon.registry.MaterialRegistry;
import cinnamon.registry.TerrainRegistry;
import cinnamon.render.MatrixStack;
import cinnamon.render.Window;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.render.shader.PostProcess;
import cinnamon.render.shader.Shaders;
import cinnamon.settings.Settings;
import cinnamon.sound.SoundManager;
import cinnamon.text.Style;
import cinnamon.text.Text;
import cinnamon.utils.*;
import cinnamon.vr.XrManager;
import cinnamon.world.collisions.Hit;
import cinnamon.world.effects.Effect;
import cinnamon.world.entity.Entity;
import cinnamon.world.entity.living.Player;
import cinnamon.world.items.CooldownItem;
import cinnamon.world.items.Inventory;
import cinnamon.world.items.Item;
import cinnamon.world.items.ItemRenderContext;
import cinnamon.world.terrain.Terrain;
import cinnamon.world.world.WorldClient;
import org.joml.Quaternionf;
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
            HUD_STYLE = new Resource("data/gui_styles/hud.json"),
            DEBUG_STYLE = new Resource("data/gui_styles/debug.json");

    protected ProgressBar health, itemCooldown;

    public void init() {
        health = new ProgressBar(0, 0, 60, 8, 1f);
        health.setColor(Colors.RED);
        health.setStyle(HUD_STYLE);

        itemCooldown = new ProgressBar(0, 0, 60, 8, 0f);
        itemCooldown.setColor(Colors.WHITE);
        itemCooldown.setStyle(HUD_STYLE);
    }

    public void tick() {}

    public void render(MatrixStack matrices, float delta) {
        Client c = Client.getInstance();

        //draw player stats
        drawPlayerStats(matrices, c.world.player, delta);

        //finish rendering
        VertexConsumer.finishAllBatches(c.camera);

        //draw crosshair separated
        if (!c.debug && !XrManager.isInXR())
            drawCrosshair(matrices);
    }

    protected void drawPlayerStats(MatrixStack matrices, Player player, float delta) {
        if (player == null)
            return;

        //draw vignette
        drawVignette(matrices, player, delta);

        //hotbar
        drawHotbar(matrices, player, delta);

        //hit direction
        drawHitDirection(matrices, player, delta);

        //draw hp and other stuff
        drawHealth(matrices, player, delta);

        //draw item stats
        drawItemStats(matrices, player.getHoldingItem(), delta);

        //effects
        drawEffects(matrices, player, delta);

        //selected terrain
        drawSelectedTerrain(matrices, delta);
    }

    protected void drawVignette(MatrixStack matrices, Player player, float delta) {
        Client c = Client.getInstance();

        float vignette = 1 - Math.min(player.getHealthProgress(), 0.3f) / 0.3f;
        int color = ((int) (vignette * 0xFF) << 24) + 0xFF0000;

        matrices.pushMatrix();
        matrices.translate(0f, 0f, -UIHelper.getDepthOffset());
        Vertex[] vertices = GeometryHelper.quad(
                matrices,
                0, 0,
                c.window.getGUIWidth(), c.window.getGUIHeight()
        );
        matrices.popMatrix();

        for (Vertex vertex : vertices)
            vertex.color(color);

        glDepthMask(false);
        VertexConsumer.MAIN.consume(vertices, VIGNETTE);
        VertexConsumer.MAIN.finishBatch(c.camera);
        glDepthMask(true);
    }

    protected void drawHealth(MatrixStack matrices, Player player, float delta) {
        Window window = Client.getInstance().window;

        //health text
        Text text = Text.of(player.getHealth() + " ").withStyle(Style.EMPTY.outlined(true).guiStyle(HUD_STYLE))
                .append(Text.of("\u2764").withStyle(Style.EMPTY.color(Colors.RED)));

        //transform matrices
        matrices.pushMatrix();
        matrices.translate(12, window.getGUIHeight() - 12, 0f);
        matrices.pushMatrix();

        matrices.rotate(Rotation.Y.rotationDeg(20f));
        matrices.rotate(Rotation.Z.rotationDeg(-10f));

        //draw text
        text.render(VertexConsumer.FONT, matrices, 0f, 0f, Alignment.BOTTOM_LEFT);

        //health progress bar
        float hp = player.getHealthProgress();
        health.setProgress(hp);
        health.render(matrices, 0, 0, delta);

        matrices.popMatrix();
        matrices.popMatrix();
    }

    protected void drawItemStats(MatrixStack matrices, Item item, float delta) {
        if (item == null)
            return;

        Window window = Client.getInstance().window;
        boolean onCooldown = item instanceof CooldownItem ci && ci.isOnCooldown();

        //item name
        Text text = Text.translated(item.getId()).withStyle(Style.EMPTY.outlined(true).guiStyle(HUD_STYLE)).append("\n");

        //item count
        if (!onCooldown) {
            Style style = Style.EMPTY.color(Colors.RED);
            text
                    .append(Text.of(item.getCount()).withStyle(style))
                    .append(" / ")
                    .append(Text.of(item.getStackCount()).withStyle(style));
        }

        //transform matrices
        matrices.pushMatrix();
        matrices.translate(window.getGUIWidth() - 12, window.getGUIHeight() - 12, 0f);
        matrices.pushMatrix();
        matrices.rotate(Rotation.Y.rotationDeg(-20f));
        matrices.rotate(Rotation.Z.rotationDeg(10f));

        //draw text
        text.render(VertexConsumer.FONT, matrices, 0f, 0f, Alignment.CENTER_RIGHT);

        //draw progressbar
        if (onCooldown) {
            itemCooldown.setProgressWithoutLerp(((CooldownItem) item).getCooldownProgress());
            itemCooldown.setPos(-itemCooldown.getWidth(), 0);
            itemCooldown.render(matrices, 0, 0, delta);
        }

        matrices.popMatrix();
        matrices.popMatrix();
    }

    protected void drawEffects(MatrixStack matrices, Player player, float delta) {
        //transform matrices
        matrices.pushMatrix();
        matrices.translate(Client.getInstance().window.getGUIWidth() - 12, 12, 0f);

        Text text = Text.empty().withStyle(Style.EMPTY.outlined(true).guiStyle(HUD_STYLE));

        for (Effect effect : player.getActiveEffects()) {
            //name
            text.append(Text.translated("effect." + effect.getType().name().toLowerCase()));

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
            text.render(VertexConsumer.FONT, matrices, 0f, 0f, Alignment.TOP_RIGHT);

        matrices.popMatrix();
    }

    protected void drawHotbar(MatrixStack matrices, Player player, float delta) {
        //set shader
        Shaders.MODEL.getShader().use().setup(Client.getInstance().camera);

        //prepare variables
        Window window = Client.getInstance().window;
        Inventory inventory = player.getInventory();
        int count = inventory.getSize();
        int selected = inventory.getSelectedIndex();

        float x = (window.getGUIWidth() - 16 * count) / 2f;
        float y = window.getGUIHeight() - 16 - 4f;

        //render items
        for (int i = 0; i < count; i++, x += 16) {
            //render slot
            VertexConsumer.MAIN.consume(GeometryHelper.quad(
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
                matrices.pushMatrix();
                matrices.translate(x + 8, y + 8, 5f);
                matrices.rotate(Rotation.Y.rotationDeg(-90));
                matrices.rotate(Rotation.X.rotationDeg(35));
                matrices.scale(-8);

                item.render(ItemRenderContext.HUD, matrices, delta);

                matrices.popMatrix();
            }
        }
    }

    protected void drawHitDirection(MatrixStack matrices, Player player, float delta) {
        int ticks = player.getDamageSourceTicks();
        if (ticks == 0)
            return;

        Float angle = player.getDamageAngle();
        if (angle == null)
            return;

        //window
        Client c = Client.getInstance();
        int w = c.window.getGUIWidth();
        int h = c.window.getGUIHeight();

        //rotate
        matrices.pushMatrix();
        matrices.translate(Math.round(w / 2f), Math.round(h / 2f), 0f);
        matrices.rotate(Rotation.Z.rotationDeg(angle));

        //draw
        Vertex[] vertices = GeometryHelper.quad(matrices, -16f, -16f, 32, 32);
        int color = ColorUtils.lerpARGBColor(0x00FFFFFF, 0xFFFFFFFF, Math.min(ticks - delta, 5) / 5f);

        for (Vertex vertex : vertices)
            vertex.color(color);

        VertexConsumer.MAIN.consume(vertices, HIT_DIRECTION);

        matrices.popMatrix();
    }

    protected void drawSelectedTerrain(MatrixStack matrices, float delta) {
        Client c = Client.getInstance();
        WorldClient w = c.world;
        int t = w.player.getSelectedTerrain();
        int m = w.player.getSelectedMaterial();

        TerrainRegistry registry = TerrainRegistry.values()[t];
        Terrain terr = registry.getFactory().get();
        MaterialRegistry material = MaterialRegistry.values()[m];
        terr.setMaterial(material);

        Vector3f bounds = terr.getAABB().getDimensions();
        Vector3f center = terr.getAABB().getCenter();
        float s = 16f / bounds.y;

        matrices.pushMatrix();
        Window ww = c.window;

        //translate to the top center of the screen
        matrices.translate(ww.getGUIWidth() * 0.5f, 4 + bounds.y * s * 0.5f, 0);
        matrices.scale(s, -s, s);

        //apply rotation for a better view angle of the model
        matrices.rotate(Rotation.X.rotationDeg(20));
        matrices.rotate(Rotation.Y.rotationDeg(-c.ticks - delta));

        //offset to center of the model
        matrices.translate(-center.x, -center.y, -center.z);

        //render terrain
        terr.render(matrices, delta);
        matrices.popMatrix();

        //render name
        Text mat = Text.translated("material." + material.name().toLowerCase());
        Text ter = Text.translated("terrain." + registry.name().toLowerCase());
        mat.append(" ").append(ter).withStyle(Style.EMPTY.shadow(true).guiStyle(HUD_STYLE)).render(VertexConsumer.FONT, matrices, ww.getGUIWidth() * 0.5f, 16 + 4 + 4, Alignment.TOP_CENTER);
    }

    protected void drawCrosshair(MatrixStack matrices) {
        Client c = Client.getInstance();

        glBlendFuncSeparate(GL_ONE_MINUS_DST_COLOR, GL_ONE_MINUS_SRC_COLOR, GL_ONE, GL_ZERO);

        VertexConsumer.MAIN.consume(GeometryHelper.quad(matrices, Math.round(c.window.getGUIWidth() / 2f - 8), Math.round(c.window.getGUIHeight() / 2f - 8), 16, 16), CROSSHAIR);
        VertexConsumer.MAIN.finishBatch(c.camera);

        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    }


    // -- debug -- //


    public static void renderDebug(MatrixStack matrices) {
        matrices.pushMatrix();
        matrices.translate(0f, 0f, 20f);

        Client c = Client.getInstance();
        Style style = Style.EMPTY.background(true).guiStyle(DEBUG_STYLE);

        //render debug text
        if (c.debug) {
            TextUtils.parseColorFormatting(Text.of(debugLeftText(c))).withStyle(style).render(VertexConsumer.FONT, matrices, 4, 4);
            TextUtils.parseColorFormatting(Text.of(debugRightText(c))).withStyle(style).render(VertexConsumer.FONT, matrices, c.window.getGUIWidth() - 4, 4, Alignment.TOP_RIGHT);

            //render crosshair
            renderDebugCrosshair(matrices);
        } else if (Settings.showFPS.get() && (c.world == null || !c.world.hudHidden())) {
            //Style style = Style.EMPTY.shadow(true);
            Text.of(c.fps + " fps @ " + c.ms + " ms").withStyle(style).render(VertexConsumer.FONT, matrices, 4, 4);
        }

        matrices.popMatrix();
    }

    private static void renderDebugCrosshair(MatrixStack matrices) {
        Client c = Client.getInstance();

        matrices.pushMatrix();
        matrices.translate(c.window.getGUIWidth() / 2f, c.window.getGUIHeight() / 2f, 0);
        if (c.world != null)
            matrices.scale(1, -1, 1);

        matrices.rotate(c.camera.getRot().invert(new Quaternionf()));

        float len = 10;
        VertexConsumer.MAIN.consume(GeometryHelper.cube(matrices, 1, 0, 0, len, 1, 1, 0xFFFF0000));
        VertexConsumer.MAIN.consume(GeometryHelper.cube(matrices, 0, 1, 0, 1, len, 1, 0xFF00FF00));
        VertexConsumer.MAIN.consume(GeometryHelper.cube(matrices, 0, 0, 1, 1, 1, len, 0xFF0000FF));

        matrices.popMatrix();
    }

    private static String debugLeftText(Client c) {
        int soundCount = SoundManager.getSoundCount();

        WorldClient w = c.world;
        if (w == null) {
            return String.format("""
                    Cinnamon v&e%s&r
                    &e%s&r fps @ &e%s&r ms

                    [&bworld&r]
                     &cNo world loaded&r
                    """, Version.CLIENT_VERSION, c.fps, c.ms);
        }

        Player p = w.player;

        Vector3f epos = p.getPos();
        Vector2f erot = p.getRot();
        Vector3f emot = p.getMotion();
        Vector3f cpos = c.camera.getPosition();
        Vector3f crot = Maths.quatToEuler(c.camera.getRotation());

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
                        Cinnamon v&e%s&r
                        &e%s&r fps @ &e%s&r ms

                        [&bworld&r]
                         &e%s&r/&e%s&r entities &e%s&r/&e%s&r particles
                         &e%s&r/&e%s&r terrain &e%s&r light sources
                         &e%s&r sounds
                         time &e%s&r
 
                        [&bplayer&r]
                         &e%s&r %s
                         x &c%.3f&r y &a%.3f&r z &b%.3f&r
                         pitch &e%.3f&r yaw &e%.3f&r
                         motion &c%.3f &a%.3f &b%.3f&r

                        [&bcamera&r]
                         x &c%.3f&r y &a%.3f&r z &b%.3f&r
                         pitch &e%.3f&r yaw &e%.3f&r roll &e%.3f&r
                         facing &e%s&r
                         mode &e%s&r

                        [&btargeted entity&r]
                        %s

                        [&btargeted terrain&r]
                        %s
                        """,
                Version.CLIENT_VERSION,
                c.fps, c.ms,

                w.getRenderedEntities(), w.entityCount(), w.getRenderedParticles(), w.particleCount(),
                w.getRenderedTerrain(), w.getExpectedRenderedTerrain(), w.lightCount(),
                soundCount,
                w.getTime(),

                p.getName(), p.getUUID(),
                epos.x, epos.y, epos.z,
                erot.x, erot.y,
                emot.x, emot.y, emot.z,

                cpos.x, cpos.y, cpos.z,
                crot.x, crot.y, crot.z,
                face,
                camera,

                entity,
                terrain
        );
    }

    private static String debugRightText(Client c) {
        Runtime r = Runtime.getRuntime();
        long max = r.maxMemory();
        long total = r.totalMemory();
        long free = r.freeMemory();
        long used = total - free;

        Window w = c.window;
        PostProcess post = c.postProcess == -1 ? null : PostProcess.EFFECTS[c.postProcess];

        return String.format("""
                [&bjava&r]
                version &e%s&r\s
                mem &e%s&r%% &e%s&r/&e%s&r\s
                allocated &e%s&r%% &e%s&r\s

                [&bsystem&r]
                OS &e%s&r\s
                %s\s
                OpenGL &e%s&r\s

                [&bwindow&r]
                &e%s&r x &e%s&r\s
                gui scale &e%s&r\s

                [&beffects&r]
                post process &e%s&r\s
                3D anaglyph &e%s&r\s
                """,
                System.getProperty("java.version"),
                used * 100 / max, Maths.prettyByteSize(used), Maths.prettyByteSize(max),
                total * 100 / max, Maths.prettyByteSize(total),

                System.getProperty("os.name"),
                glGetString(GL_RENDERER),
                glGetString(GL_VERSION),

                w.width, w.height,
                w.guiScale,

                post == null ? "none" : post.name(),
                c.anaglyph3D ? "on" : "off"
        );
    }

    private static String getTargetedObjString(Hit<? extends WorldObject> hit, float range) {
        if (hit == null)
            return " ---";

        Vector3f pos = hit.obj().getPos();
        Vector3f hPos = hit.pos();
        Vector3f normal = hit.collision().normal();
        float distance = range * hit.collision().near();
        String type = hit.obj().getType().name();
        String extra = (hit.obj() instanceof Entity e) ? "\n uuid &e" + e.getUUID() + "&r" : (hit.obj() instanceof Terrain t) ? "\n rotation &e" + (int) t.getRotationAngle() + "&r" : "";
        return String.format("""
                 x &c%.3f&r y &a%.3f&r z &b%.3f&r
                 hit pos x &c%.3f&r y &a%.3f&r z &b%.3f&r
                 hit normal x &c%.3f&r y &a%.3f&r z &b%.3f&r
                 hit distance &e%.3fm&r
                 type &e%s&r%s
                """,
                pos.x, pos.y, pos.z,
                hPos.x, hPos.y, hPos.z,
                normal.x, normal.y, normal.z,
                distance,
                type,
                extra
        );
    }
}
