package cinnamon.world.worldgen;

import cinnamon.Client;
import cinnamon.world.world.WorldClient;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_F6;

public class GeneratedWorld extends WorldClient {

    private final String worldName;
    private final long seed;
    private ChunkManager chunks;
    // simple debounce to avoid thrashing while sprinting
    private int ensureCooldown = 0;

    // chunk radius config
    private int radiusXY = 1; // small radius for testing
    private int radiusY = 1;

    public GeneratedWorld(String worldName, long seed) {
        this.worldName = worldName;
        this.seed = seed;
    }

    @Override
    protected void tempLoad() {
        // start streaming chunks instead of static TerrainGenerator content
        this.chunks = new ChunkManager(this, worldName, seed);
        this.chunks.setMaterializeBudgetPerTick(4);
        // initial ensure in case player exists already (init() creates player)
        ensureAroundPlayer();
    }

    private void ensureAroundPlayer() {
    if (player == null) return;
        int cx = (int) Math.floor(player.getPos().x / ChunkManager.CHUNK_SIZE);
        int cy = Math.max(0, (int) Math.floor(player.getPos().y / ChunkManager.CHUNK_SIZE));
        int cz = (int) Math.floor(player.getPos().z / ChunkManager.CHUNK_SIZE);
        chunks.ensureRadius(cx, cy, cz, radiusXY, radiusY);
    }

    @Override
    public void tick() {
        super.tick();
        if (chunks != null) {
            if (ensureCooldown-- <= 0) {
                ensureAroundPlayer();
                ensureCooldown = 5; // ~5 ticks debounce
            }
            chunks.tick();
        }
    }

    @Override
    public void keyPress(int key, int scancode, int action, int mods) {
        super.keyPress(key, scancode, action, mods);
        if (action == 1 && key == GLFW_KEY_F6 && chunks != null) { // GLFW_PRESS
            chunks.saveAllDirty();
            Client.LOGGER.info("World saved.");
        }
    }

    @Override
    public void close() {
        if (chunks != null) {
            chunks.saveAllDirty();
            chunks.shutdown();
        }
        super.close();
    }
}
