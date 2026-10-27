package cinnamon.world.gui;

import cinnamon.Client;
import cinnamon.gui.DebugScreen;
import cinnamon.gui.GUISkin;
import cinnamon.gui.screens.world.ChatScreen;
import cinnamon.gui.widgets.types.OverlayProgressBar;
import cinnamon.gui.widgets.types.ProgressBar;
import cinnamon.math.Maths;
import cinnamon.messages.Message;
import cinnamon.messages.MessageManager;
import cinnamon.model.GeometryHelper;
import cinnamon.model.Vertex;
import cinnamon.registry.CoreShadersRegistry;
import cinnamon.registry.MaterialRegistry;
import cinnamon.registry.TerrainRegistry;
import cinnamon.render.MatrixStack;
import cinnamon.render.Window;
import cinnamon.render.WorldRenderer;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.text.Style;
import cinnamon.text.Text;
import cinnamon.utils.Alignment;
import cinnamon.utils.Colors;
import cinnamon.utils.Resource;
import cinnamon.utils.TextUtils;
import cinnamon.utils.UIHelper;
import cinnamon.vr.XrManager;
import cinnamon.world.Abilities;
import cinnamon.world.effects.Effect;
import cinnamon.world.entity.living.LivingEntity;
import cinnamon.world.entity.living.LocalPlayer;
import cinnamon.world.entity.living.Player;
import cinnamon.world.items.CooldownItem;
import cinnamon.world.items.Inventory;
import cinnamon.world.items.Item;
import cinnamon.world.items.ItemRenderContext;
import cinnamon.world.terrain.Terrain;
import org.joml.Math;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL14.glBlendFuncSeparate;

public class Hud {

    public static final Resource
            SKIN          = new Resource("data/gui_skins/hud.json"),
            VIGNETTE      = new Resource("textures/gui/hud/vignette.png"),
            CROSSHAIR     = new Resource("textures/gui/hud/crosshair.png"),
            HIT_DIRECTION = new Resource("textures/gui/hud/hit_direction.png"),
            BACKGROUND    = new Resource("textures/gui/hud/panel.png"),
            HEALTH_HEART  = new Resource("textures/gui/hud/heart_bar.png"),
            HEALTH_SEG    = new Resource("textures/gui/hud/health_segment.png"),
            HOTBAR_BG     = new Resource("textures/gui/hud/hotbar_panel.png"),
            HOTBAR        = new Resource("textures/gui/hud/hotbar.png");

    protected ProgressBar health;
    protected ProgressBar[] healthSegs = new OverlayProgressBar[10];
    protected ProgressBar itemCooldown;

    protected Terrain terrain;

    protected boolean fadeIn = false;
    protected int fadeTicks = 0, fadeDelay = 20;
    protected int fadeColor = 0xFFFFFF;

    protected final List<Marker> markers = new ArrayList<>();

    public void init() {
        health = new OverlayProgressBar(0, 0, 8, 8, 1f);
        health.setSkin(SKIN);
        ((OverlayProgressBar) health).setIcon(HEALTH_HEART);
        ((OverlayProgressBar) health).setVertical(true);
        ((OverlayProgressBar) health).setInverted(true);

        for (int i = 0; i < healthSegs.length; i++) {
            healthSegs[i] = new OverlayProgressBar(0, 0, 8, 8, 1f);
            healthSegs[i].setSkin(SKIN);
            ((OverlayProgressBar) healthSegs[i]).setIcon(HEALTH_SEG);
        }

        itemCooldown = new ProgressBar(0, 0, 60, 8, 0f);
        itemCooldown.setColor(Colors.WHITE);
        itemCooldown.setSkin(SKIN);
    }

    public void free() {}

    public void tick() {
        //tick markers
        for (Iterator<Marker> iterator = markers.iterator(); iterator.hasNext(); ) {
            Marker marker = iterator.next();
            marker.tick();
            if (marker.isRemoved())
                iterator.remove();
        }

        //tick fade
        if (fadeIn && fadeTicks < fadeDelay)
            fadeTicks++;
        else if (!fadeIn && fadeTicks > 0)
            fadeTicks--;
    }

    public void render(MatrixStack matrices, float delta) {
        Client c = Client.getInstance();

        //draw player stats
        if (c.world != null && c.world.playerEntity instanceof LivingEntity entity)
            drawEntityStats(matrices, entity, delta);

        //draw marker
        drawMarkers(matrices, delta);

        //draw chat last
        drawRecentChat(matrices, c, delta);

        //draw fade
        if (!XrManager.isInXR() && fadeTicks > 0)
            drawFade(matrices, c, delta);

        //finish rendering
        WorldRenderer.finishMaterials(c.camera);

        //draw crosshair separated
        if (!DebugScreen.isActive() && !XrManager.isInXR())
            drawCrosshair(matrices);
    }

    protected void drawEntityStats(MatrixStack matrices, LivingEntity entity, float delta) {
        if (entity == null)
            return;

        //draw vignette
        drawVignette(matrices, entity, delta);

        //selected terrain
        if (entity instanceof LocalPlayer player)
            drawSelectedTerrain(matrices, player, delta);

        //hotbar
        drawHotbar(matrices, entity, delta);

        //hit direction
        if (entity instanceof Player player)
            drawHitDirection(matrices, player, delta);

        //draw hp and other stuff
        drawHealth(matrices, entity, delta);

        //draw item stats
        drawItemStats(matrices, entity.getHoldingItem(), delta);

        //effects
        drawEffects(matrices, entity, delta);
    }

    protected void drawVignette(MatrixStack matrices, LivingEntity entity, float delta) {
        Client c = Client.getInstance();

        float vignette = 1 - Math.min(entity.getHealthProgress(), 0.3f) / 0.3f;
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

    protected void drawHealth(MatrixStack matrices, LivingEntity entity, float delta) {
        if (entity instanceof Player player && player.getAbilities().get(Abilities.Ability.GOD_MODE))
            return;

        Window window = Client.getInstance().window;
        Text text = Text.of(entity.getHealth()).withStyle(Style.EMPTY.outlined(true).guiSkin(SKIN));
        float hp = entity.getHealthProgress();
        int segWidth = healthSegs[0].getWidth();
        int segHeight = healthSegs[0].getHeight();
        int segCount = healthSegs.length;

        //transform matrices
        matrices.pushMatrix();
        matrices.translate(12, window.getGUIHeight() - 12 - segHeight, 0f);

        //text background
        UIHelper.nineQuad(
                VertexConsumer.MAIN, matrices, BACKGROUND,
                -4f, -4f - segHeight - 6f,
                TextUtils.getWidth(text) + health.getWidth() + 10f, TextUtils.getHeight(text) + 8f,
                0f, 0f,
                16, 16,
                16, 16
        );

        //background
        UIHelper.nineQuad(
                VertexConsumer.MAIN, matrices, BACKGROUND,
                -4, health.getHeight() - segHeight - 4f,
                (segCount * (segWidth + 1f) - 1f) + 8f, segHeight + 8f,
                0f, 0f,
                16, 16,
                16, 16
        );

        //health segments
        matrices.pushMatrix();
        for (int i = 0; i < segCount; i++) {
            ProgressBar seg = healthSegs[i];
            seg.setProgress(Math.min(Math.max(hp * segCount - i, 0f), 1f));
            seg.render(matrices, 0, 0, delta);
            matrices.translate(seg.getWidth() + 1, 0, 0);
        }
        matrices.popMatrix();

        //health progress bar
        matrices.pushMatrix();
        matrices.translate(0, -segHeight - health.getHeight() + 2f, 0f);
        health.setProgress(hp);
        health.render(matrices, 0, 0, delta);
        matrices.popMatrix();

        //draw text
        text.render(VertexConsumer.MAIN, matrices, health.getWidth() + 2f, -segHeight + 2f, Alignment.BOTTOM_LEFT);

        matrices.popMatrix();
    }

    protected void drawItemStats(MatrixStack matrices, Item item, float delta) {
        if (item == null)
            return;

        Window window = Client.getInstance().window;
        boolean onCooldown = item instanceof CooldownItem ci && ci.isOnCooldown();

        //item name
        Text text = Text.translated(item.getId()).withStyle(Style.EMPTY.shadow(true).guiSkin(SKIN)).append("\n");

        //item count
        if (!onCooldown)
            text.append(Text.empty().append(item.getCountText()).withStyle(Style.EMPTY.color(text.getStyle().getGuiSkin().getInt("accent_color"))));

        int textWidth = TextUtils.getWidth(text);
        int textHeight = TextUtils.getHeight(text);
        itemCooldown.setWidth(textWidth);

        //transform matrices
        matrices.pushMatrix();
        matrices.translate(window.getGUIWidth() - 12, window.getGUIHeight() - 12 - textHeight, 0f);

        //render background
        UIHelper.nineQuad(
                VertexConsumer.MAIN, matrices, BACKGROUND,
                -textWidth - 4f, -4f,
                textWidth + 8f, textHeight + 8f,
                0f, 0f,
                16, 16,
                16, 16
        );

        //draw progressbar
        if (onCooldown) {
            matrices.pushMatrix();
            matrices.translate(-textWidth, textHeight / 2f, 0f);
            itemCooldown.setProgressWithoutLerp(((CooldownItem) item).getCooldownProgress());
            itemCooldown.render(matrices, 0, 0, delta);
            matrices.popMatrix();
        }

        //draw text
        text.render(VertexConsumer.MAIN, matrices, 0f, 0f, Alignment.TOP_RIGHT);

        matrices.popMatrix();
    }

    protected void drawEffects(MatrixStack matrices, LivingEntity entity, float delta) {
        if (entity.getActiveEffects().isEmpty())
            return;

        //transform matrices
        matrices.pushMatrix();
        matrices.translate(Client.getInstance().window.getGUIWidth() - 12, 12, 0f);


        for (Effect effect : entity.getActiveEffects()) {
            //name
            Text text = Text.translated("effect." + effect.getType().name().toLowerCase()).withStyle(Style.EMPTY.shadow(true).guiSkin(SKIN));

            //amplitude
            int amplitude = effect.getAmplitude();
            if (amplitude != 1)
                text.append(" ").append(Text.translated("effect.amplitude", amplitude));

            //divider
            text.append(": ");

            //remaining time
            text.append(Text.of((effect.getRemainingTime()) / 20).withStyle(Style.EMPTY.color(text.getStyle().getGuiSkin().getInt("accent_color"))));

            //render
            int width = TextUtils.getWidth(text);
            int height = TextUtils.getHeight(text);

            //background
            UIHelper.nineQuad(
                    VertexConsumer.MAIN, matrices, BACKGROUND,
                    -width - 4f, -4f,
                    width + 8f, height + 8f,
                    0f, 0f,
                    16, 16,
                    16, 16
            );

            //text
            text.render(VertexConsumer.MAIN, matrices, 0f, 0f, Alignment.TOP_RIGHT);

            matrices.translate(0f, height + 8f - 2f, 0f);
        }

        matrices.popMatrix();
    }

    protected void drawHotbar(MatrixStack matrices, LivingEntity entity, float delta) {
        //set shader
        CoreShadersRegistry.MODEL.getShader().use().setup(Client.getInstance().camera);

        //prepare variables
        Window window = Client.getInstance().window;
        Inventory inventory = entity.getInventory();
        int count = inventory.getHotbarSize();
        int selected = inventory.getSelectedIndex();

        float x = (window.getGUIWidth() - 16 * count) / 2f;
        float y = window.getGUIHeight() - 16 - 8f;

        //render background
        UIHelper.nineQuad(
                VertexConsumer.MAIN, matrices, HOTBAR_BG,
                x - 4f, y - 2f,
                16 * count + 8f, 16 + 4f,
                0f, 0f,
                16, 16,
                16, 16
        );

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
                matrices.rotateY(-90);
                matrices.rotateX(35);
                matrices.scale(-8);

                item.render(ItemRenderContext.HUD, matrices, delta);

                matrices.popMatrix();

                if (item.getStackSize() > 1) {
                    matrices.pushMatrix();
                    matrices.translate(0f, 0f, Math.max(15f, UIHelper.getDepthOffset()));
                    Text.of(item.getCount()).withStyle(Style.EMPTY.outlined(true).guiSkin(SKIN))
                            .render(VertexConsumer.MAIN, matrices, x + 16, y + 16 + 2, Alignment.BOTTOM_RIGHT);
                    matrices.popMatrix();
                }
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
        matrices.rotateZ(angle);

        //draw
        Vertex[] vertices = GeometryHelper.quad(matrices, -16f, -16f, 32, 32);
        int alpha = (int) (Math.min(ticks - delta, 5) / 5f * 0xFF);
        int color = (alpha << 24) + Colors.RED.rgb;

        for (Vertex vertex : vertices)
            vertex.color(color);

        VertexConsumer.MAIN.consume(vertices, HIT_DIRECTION);

        matrices.popMatrix();
    }

    protected void drawSelectedTerrain(MatrixStack matrices, LocalPlayer player, float delta) {
        Client c = Client.getInstance();
        if (!player.getAbilities().get(Abilities.Ability.CAN_BUILD))
            return;

        int t = player.getSelectedTerrain();
        int m = player.getSelectedMaterial();

        TerrainRegistry registry = TerrainRegistry.values()[t];
        if (terrain == null || terrain.getType() != registry) {
            terrain = registry.getFactory().get();
            terrain.calculateBounds();
        }

        MaterialRegistry material = MaterialRegistry.values()[m];
        terrain.setMaterial(material.getMaterial());

        Text mat = Text.translated("material." + material.name().toLowerCase());
        Text ter = Text.translated("terrain." + registry.name().toLowerCase());
        mat.append(" ").append(ter).withStyle(Style.EMPTY.shadow(true).guiSkin(SKIN));

        Vector3f bounds = terrain.getAABB().getDimensions();
        Vector3f center = terrain.getAABB().getCenter();
        float maxDim = Math.max(bounds.x, Math.max(bounds.y, bounds.z));

        float modelSize = 8f;
        float s = modelSize / maxDim;

        float textWidth = TextUtils.getWidth(mat);
        float textHeight = TextUtils.getHeight(mat);
        float spacing = 4f;
        float paddingX = 6f;
        float paddingY = 4f;

        float contentWidth = modelSize + spacing + textWidth;
        float contentHeight = Math.max(modelSize, textHeight);

        matrices.pushMatrix();
        Window ww = c.window;

        //translate matrices
        matrices.translate(ww.getGUIWidth() * 0.5f, ww.getGUIHeight() - 32f, -modelSize);

        //render background
        UIHelper.nineQuad(
                VertexConsumer.MAIN, matrices, BACKGROUND,
                Math.round(-contentWidth / 2f - paddingX), Math.round(-contentHeight / 2f - paddingY),
                Math.round(contentWidth + paddingX * 2f), Math.round(contentHeight + paddingY * 2f),
                0f, 0f,
                16, 16,
                16, 16
        );
        matrices.translate(0f, 0f, modelSize);

        //render model
        matrices.pushMatrix();

        //center model locally
        matrices.translate(-contentWidth / 2f + modelSize / 2f, 0f, 0f);
        matrices.scale(s, -s, s);

        //apply rotation animation
        matrices.rotateX(20);
        matrices.rotateY(-c.ticks - delta);

        //offset to center of the model
        matrices.translate(-center.x, -center.y, -center.z);

        //render terrain
        CoreShadersRegistry.MODEL.getShader().use().setup(c.camera);
        terrain.render(c.camera, matrices, delta);
        matrices.popMatrix();

        //render name
        float textCenterX = -contentWidth / 2f + modelSize + spacing + textWidth / 2f;
        mat.render(VertexConsumer.MAIN, matrices, textCenterX, 0, Alignment.CENTER);

        matrices.popMatrix();
    }

    protected void drawFade(MatrixStack matrices, Client client, float delta) {
        float fadeProgress = (fadeTicks + (fadeIn ? delta : -delta)) / fadeDelay;
        int alpha = (int) (Maths.clamp(fadeProgress, 0f, 1f) * 0xFF);
        int color = (alpha << 24) + fadeColor;
        VertexConsumer.MAIN.consume(GeometryHelper.rectangle(matrices, 0, 0, client.window.getGUIWidth(), client.window.getGUIHeight(), color));
    }

    public void setFade(boolean fadeIn) {
        this.fadeIn = fadeIn;
        this.fadeTicks = fadeIn ? 0 : this.fadeDelay;
    }

    public void setFade(boolean fadeIn, int delay, int color) {
        this.fadeDelay = delay;
        this.fadeColor = color;
        this.setFade(fadeIn);
    }

    protected void drawCrosshair(MatrixStack matrices) {
        Client c = Client.getInstance();

        glBlendFuncSeparate(GL_ONE_MINUS_DST_COLOR, GL_ONE_MINUS_SRC_COLOR, GL_ONE, GL_ZERO);

        VertexConsumer.MAIN.consume(GeometryHelper.quad(matrices,
                Math.round(c.window.getGUIWidth() / 2f - 8), Math.round(c.window.getGUIHeight() / 2f - 8),
                16, 16,
                0f, 0f,
                16f, 16f,
                64, 16
        ), CROSSHAIR);
        VertexConsumer.MAIN.finishBatch(c.camera);

        glBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
    }

    protected void drawRecentChat(MatrixStack matrices, Client client, float delta) {
        if (client.screen instanceof ChatScreen)
            return;

        int maxChatWidth = 350;
        int displayDurationTicks = 13 * Client.TPS; //13s
        int alphaDecayStart = 10 * Client.TPS; //10s
        long timeNow = client.ticks;

        //render messages up to the max recent messages limit
        List<Message> messages = MessageManager.getMessages();
        int start = Math.max(0, messages.size() - MessageManager.RECENT_MESSAGES);

        //starting coordinates
        float x = 4 + 2;
        float y = client.window.getGUIHeight() - GUISkin.getCurrentSkin().getFont().lineHeight - 2 - 24 - 4 - 2;
        int maxLines = MessageManager.RECENT_MESSAGES;

        for (int i = messages.size() - 1; i >= start && maxLines > 0; i--) {
            Message msg = messages.get(i);
            float age = timeNow - msg.addedTime() + delta;

            //skip messages older than our duration limit
            if (age > displayDurationTicks)
                break;

            //calculate alpha based on age
            final int alpha;
            if (age > alphaDecayStart) {
                float decayProgress = (age - alphaDecayStart) / (displayDurationTicks - alphaDecayStart);
                alpha = (int) ((1 - decayProgress) * 0xFF);
            } else {
                alpha = 0xFF;
            }

            //split the message at new lines
            Text text = msg.text();
            List<Text> split = TextUtils.split(text, "\n");
            List<Text> warped = new ArrayList<>();

            for (Text t : split) {
                //apply color to each text segment while preserving styles
                if (alpha < 0xFF)
                    t.visitStyle((str, style) -> style
                            .color((alpha << 24) | (style.getColor() & 0xFFFFFF))
                            .shadowColor((alpha << 24) | (style.getShadowColor() & 0xFFFFFF))
                            .backgroundColor((alpha << 24) | (style.getBackgroundColor() & 0xFFFFFF))
                    );

                //warp the whole text and add to the list
                warped.addAll(TextUtils.warpToWidth(t, maxChatWidth - 4));
            }

            //render warped lines bottom-to-top
            for (int j = warped.size() - 1; j >= 0 && maxLines > 0; j--) {
                Text warpedMessage = warped.get(j);
                int messageHeight = TextUtils.getHeight(warpedMessage);

                //render the message
                warpedMessage.render(VertexConsumer.MAIN, matrices, x, y, Alignment.BOTTOM_LEFT);

                //move up for the next message
                y -= messageHeight;
                maxLines--;
            }

            y -= 2; //spacing between messages
        }
    }

    protected void drawMarkers(MatrixStack matrices, float delta) {
        for (Marker marker : markers)
            marker.render(matrices, delta);
    }

    public void addMarker(Marker marker) {
        markers.add(marker);
    }
}
