package cinnamon.world;

import cinnamon.Client;
import cinnamon.gui.DebugScreen;
import cinnamon.gui.widgets.types.ProgressBar;
import cinnamon.model.GeometryHelper;
import cinnamon.model.Vertex;
import cinnamon.registry.MaterialRegistry;
import cinnamon.registry.TerrainRegistry;
import cinnamon.render.MatrixStack;
import cinnamon.render.Window;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.render.shader.Shaders;
import cinnamon.text.Style;
import cinnamon.text.Text;
import cinnamon.utils.*;
import cinnamon.vr.XrManager;
import cinnamon.world.effects.Effect;
import cinnamon.world.entity.living.Player;
import cinnamon.world.items.CooldownItem;
import cinnamon.world.items.Inventory;
import cinnamon.world.items.Item;
import cinnamon.world.items.ItemRenderContext;
import cinnamon.world.terrain.Terrain;
import cinnamon.world.world.WorldClient;
import org.joml.Math;
import org.joml.Vector3f;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL14.glBlendFuncSeparate;

public class Hud {

    public static final Resource
            CROSSHAIR = new Resource("textures/gui/hud/crosshair.png"),
            HOTBAR = new Resource("textures/gui/hud/hotbar.png"),
            VIGNETTE = new Resource("textures/gui/hud/vignette.png"),
            HIT_DIRECTION = new Resource("textures/gui/hud/hit_direction.png"),
            HUD_STYLE = new Resource("data/gui_styles/hud.json");

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
        if (!DebugScreen.isActive() && !XrManager.isInXR())
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
        text.render(VertexConsumer.MAIN, matrices, 0f, 0f, Alignment.BOTTOM_LEFT);

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
        if (!onCooldown)
            text.append(Text.empty().append(item.getCountText()).withStyle(Style.EMPTY.color(Colors.RED)));

        //transform matrices
        matrices.pushMatrix();
        matrices.translate(window.getGUIWidth() - 12, window.getGUIHeight() - 12, 0f);
        matrices.pushMatrix();
        matrices.rotate(Rotation.Y.rotationDeg(-20f));
        matrices.rotate(Rotation.Z.rotationDeg(10f));

        //draw text
        text.render(VertexConsumer.MAIN, matrices, 0f, 0f, Alignment.CENTER_RIGHT);

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
            text.render(VertexConsumer.MAIN, matrices, 0f, 0f, Alignment.TOP_RIGHT);

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
        if (!w.player.getAbilities().canBuild())
            return;

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
        terr.render(c.camera, matrices, delta);
        matrices.popMatrix();

        //render name
        Text mat = Text.translated("material." + material.name().toLowerCase());
        Text ter = Text.translated("terrain." + registry.name().toLowerCase());
        mat.append(" ").append(ter).withStyle(Style.EMPTY.shadow(true).guiStyle(HUD_STYLE)).render(VertexConsumer.MAIN, matrices, ww.getGUIWidth() * 0.5f, 16 + 4 + 4, Alignment.TOP_CENTER);
    }

    protected void drawCrosshair(MatrixStack matrices) {
        Client c = Client.getInstance();

        glBlendFuncSeparate(GL_ONE_MINUS_DST_COLOR, GL_ONE_MINUS_SRC_COLOR, GL_ONE, GL_ZERO);

        VertexConsumer.MAIN.consume(GeometryHelper.quad(matrices, Math.round(c.window.getGUIWidth() / 2f - 8), Math.round(c.window.getGUIHeight() / 2f - 8), 16, 16), CROSSHAIR);
        VertexConsumer.MAIN.finishBatch(c.camera);

        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    }
}
