package cinnamon.math.noise;

import cinnamon.math.Maths;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

public abstract class Noise {

    public static int DEFAULT_SIZE = 256;

    protected final int width, height, depth;
    protected final long seed;
    protected final ByteBuffer buffer;

    public Noise(int width, int height, long seed) {
        this(width, height, 1, seed);
    }

    public Noise(int width, int height, int depth, long seed) {
        this.width = width;
        this.height = height;
        this.depth = depth;
        this.seed = seed;
        this.buffer = MemoryUtil.memAlloc(width * height * depth);
    }

    protected abstract void build();

    public void free() {
        MemoryUtil.memFree(buffer);
    }

    public float sample(int x, int y) {
        //wrap coordinates
        int px = Maths.modulo(x, width);
        int py = Maths.modulo(y, height);

        //get byte value and normalize to [0, 1]
        int value = buffer.get(px + py * width) & 0xFF;
        return value / 255f;
    }

    public float sample(int x, int y, int z) {
        int px = Maths.modulo(x, width);
        int py = Maths.modulo(y, height);
        int pz = Maths.modulo(z, depth);

        int value = buffer.get(px + py * width + pz * width * height) & 0xFF;
        return value / 255f;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getDepth() {
        return depth;
    }

    public long getSeed() {
        return seed;
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }
}
