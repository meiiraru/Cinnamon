package mayo.world.chunk;

import mayo.world.World;

public class Chunk {
    public static final int CHUNK_SIZE = 32;

    // -- temp -- //

    //todo - definitely not static, and biome dependent
    public static final float fogDensity = 0.5f;

    public static final int fogColor = 0xC1E7FF;
    public static final int ambientLight = 0xFFFFFF;

    public static float getFogStart(World world) {
        return CHUNK_SIZE * world.renderDistance;
    }
    public static float getFogEnd(World world) {
        return CHUNK_SIZE * world.renderDistance + (CHUNK_SIZE / fogDensity);
    }

    // -- end temp -- //
}
