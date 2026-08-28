package cinnamon.math.noise;

import cinnamon.math.Maths;
import org.joml.Math;
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

    public float sample(float x, float y) {
        int x0 = (int) Math.floor(x);
        int y0 = (int) Math.floor(y);

        float fx = x - x0;
        float fy = y - y0;

        float c00 = sample(x0, y0);
        float c10 = sample(x0 + 1, y0);
        float c01 = sample(x0, y0 + 1);
        float c11 = sample(x0 + 1, y0 + 1);

        float nx0 = Math.lerp(c00, c10, fx);
        float nx1 = Math.lerp(c01, c11, fx);
        return Math.lerp(nx0, nx1, fy);
    }

    public float sample(float x, float y, float z) {
        int x0 = (int) Math.floor(x);
        int y0 = (int) Math.floor(y);
        int z0 = (int) Math.floor(z);

        float fx = x - x0;
        float fy = y - y0;
        float fz = z - z0;

        float c000 = sample(x0, y0, z0);
        float c100 = sample(x0 + 1, y0, z0);
        float c010 = sample(x0, y0 + 1, z0);
        float c110 = sample(x0 + 1, y0 + 1, z0);
        float c001 = sample(x0, y0, z0 + 1);
        float c101 = sample(x0 + 1, y0, z0 + 1);
        float c011 = sample(x0, y0 + 1, z0 + 1);
        float c111 = sample(x0 + 1, y0 + 1, z0 + 1);

        float nx00 = Math.lerp(c000, c100, fx);
        float nx10 = Math.lerp(c010, c110, fx);
        float nx01 = Math.lerp(c001, c101, fx);
        float nx11 = Math.lerp(c011, c111, fx);

        float nxy0 = Math.lerp(nx00, nx10, fy);
        float nxy1 = Math.lerp(nx01, nx11, fy);

        return Math.lerp(nxy0, nxy1, fz);
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
