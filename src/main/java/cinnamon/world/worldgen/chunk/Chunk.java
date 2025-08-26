package cinnamon.world.worldgen.chunk;

import cinnamon.render.Camera;
import cinnamon.render.MatrixStack;
import cinnamon.render.WorldRenderer;
import cinnamon.utils.AABB;
import cinnamon.world.terrain.Terrain;
import cinnamon.world.world.World;
import org.joml.Vector3i;

import java.util.Collection;

public abstract class Chunk {
    public static final int CHUNK_SIZE = 32;

    // -- temp -- //

    //todo - definitely not static, should be biome dependent
    public static final float fogDensity = 0.5f;

    public static final int fogColor = 0xC1E7FF;
    public static final int ambientLight = 0xFFFFFF;

    public static float getFogStart() {
        return CHUNK_SIZE * (WorldRenderer.renderDistance - 2);
    }
    public static float getFogEnd() {
        return CHUNK_SIZE * (WorldRenderer.renderDistance - 2) + (CHUNK_SIZE / fogDensity);
    }

    protected final Vector3i gridPos;
    protected final AABB aabb;

    public Chunk(int x, int y, int z) {
        this.gridPos = new Vector3i(x, y, z);
        int ax = x * CHUNK_SIZE;
        int ay = y * CHUNK_SIZE;
        int az = z * CHUNK_SIZE;
        this.aabb = new AABB(ax, ay, az, ax + CHUNK_SIZE, ay + CHUNK_SIZE, az + CHUNK_SIZE);
    }

    // -- end temp -- //

    public abstract void tick();

    public abstract int render(Camera camera, MatrixStack matrices, float delta);

    public abstract void onAdded(World world);

    public abstract Terrain getTerrainAtPos(float x, float y, float z);

    public abstract void setTerrain(Terrain terrain, float x, float y, float z);

    public abstract Collection<Terrain> getTerrainInArea(AABB area);

    public boolean shouldRender(Camera camera) {
        return camera.isInsideFrustum(getAABB());
    }

    public AABB getAABB() {
        return aabb;
    }

    public Vector3i getGridPos() {
        return gridPos;
    }
}
