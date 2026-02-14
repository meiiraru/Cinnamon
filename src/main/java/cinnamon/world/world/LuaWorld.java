package cinnamon.world.world;

import cinnamon.Client;
import cinnamon.model.GeometryHelper;
import cinnamon.registry.MaterialRegistry;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.scripting.LuaChatScreen;
import cinnamon.scripting.LuaEngine;
import cinnamon.scripting.api.LuaHudLib;
import cinnamon.text.Style;
import cinnamon.text.Text;
import cinnamon.utils.Alignment;
import cinnamon.utils.Colors;
import cinnamon.world.Hud;
import cinnamon.world.worldgen.TerrainGenerator;

/**
 * A world that hosts a Lua scripting engine, allowing users to write and run
 * Lua scripts that interact with the game. Open chat (Enter/T) to type Lua code.
 */
public class LuaWorld extends WorldClient {

    private LuaEngine luaEngine;

    @Override
    protected void tempLoad() {
        // Generate a flat grass floor
        TerrainGenerator.fill(this, -24, 0, -24, 24, 0, 24, MaterialRegistry.GRASS);

        // Initialize scripting engine
        luaEngine = new LuaEngine(this);

        // Override chat screen to use Lua console
        this.chatScreen = () -> new LuaChatScreen(luaEngine);

        // Use custom HUD
        this.hud = new LuaWorldHud();
        this.hud.init();

        // Load startup script
        String result = luaEngine.executeResource("lua_world_init.lua");
        if (result != null && result.startsWith("Error:")) {
            LuaEngine.LOGGER.error("Init script: " + result);
        }
    }

    @Override
    public void respawn(boolean init) {
        super.respawn(init);
        player.setPos(0.5f, 1.5f, 8f);
        player.setRot(0f, 0f);
    }

    @Override
    public void close() {
        if (luaEngine != null) {
            luaEngine.close();
            luaEngine = null;
        }
        super.close();
    }

    public LuaEngine getLuaEngine() {
        return luaEngine;
    }

    // -- Custom HUD --

    private class LuaWorldHud extends Hud {

        @Override
        public void render(MatrixStack matrices, float delta) {
            Client c = Client.getInstance();

            // Render standard HUD elements
            if (c.world != null && c.world.player != null) {
                drawHealth(matrices, c.world.player, delta);
            }

            // Render Lua HUD elements
            renderLuaHud(matrices);

            // Show console hint
            Text.of("[Enter/T] Lua Console")
                    .withStyle(Style.EMPTY.outlined(true).guiStyle(HUD_STYLE).color(Colors.LIGHT_GRAY))
                    .render(VertexConsumer.MAIN, matrices, 4, 4, Alignment.TOP_LEFT);

            // Flush
            VertexConsumer.finishAllBatches(c.camera);
        }

        private void renderLuaHud(MatrixStack matrices) {
            if (luaEngine == null) return;

            LuaHudLib hudLib = (LuaHudLib) luaEngine.getGlobals().get("hud");
            if (hudLib == null) return;

            for (var entry : hudLib.getElements().entrySet()) {
                LuaHudLib.HudElement element = entry.getValue();

                if (element instanceof LuaHudLib.HudText text) {
                    Text.of(text.text)
                            .withStyle(Style.EMPTY.outlined(true).guiStyle(HUD_STYLE).color(text.color))
                            .render(VertexConsumer.MAIN, matrices, text.x, text.y, Alignment.TOP_LEFT);

                } else if (element instanceof LuaHudLib.HudProgressBar bar) {
                    // Background
                    VertexConsumer.MAIN.consume(
                            GeometryHelper.rectangle(matrices, bar.x, bar.y, bar.x + bar.width, bar.y + bar.height, 0xFF222222)
                    );
                    // Fill
                    int fillWidth = (int) (bar.width * Math.min(1f, Math.max(0f, bar.progress)));
                    if (fillWidth > 0) {
                        VertexConsumer.MAIN.consume(
                                GeometryHelper.rectangle(matrices, bar.x, bar.y, bar.x + fillWidth, bar.y + bar.height, bar.color)
                        );
                    }
                }
            }
        }
    }
}
