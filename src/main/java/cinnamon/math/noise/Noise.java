package cinnamon.math.noise;

import cinnamon.math.Maths;
import org.joml.Math;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

/**
 * Abstract class for generating and sampling noise textures
 */
public abstract class Noise {

    public static int DEFAULT_SIZE = 256;

    protected final int width, height, depth;
    protected final long seed;
    protected final ByteBuffer buffer;

    /**
     * Constructs a 2D noise texture with the given width, height, and seed
     * @param width The width of the noise texture
     * @param height The height of the noise texture
     * @param seed The seed for the noise generation
     * @see #Noise(int, int, int, long)
     */
    public Noise(int width, int height, long seed) {
        this(width, height, 1, seed);
    }

    /**
     * Constructs a 3D noise texture with the given width, height, depth, and seed
     * @param width The width of the noise texture
     * @param height The height of the noise texture
     * @param depth The depth of the noise texture
     * @param seed The seed for the noise generation
     * @see #Noise(int, int, long)
     */
    public Noise(int width, int height, int depth, long seed) {
        this.width = width;
        this.height = height;
        this.depth = depth;
        this.seed = seed;
        this.buffer = MemoryUtil.memAlloc(width * height * depth);
    }

    protected abstract void build();

    /**
     * Frees the memory allocated for the noise texture buffer<br>
     * This method should be called only when the noise texture is no longer accessed and needed to avoid any memory leaks
     */
    public void free() {
        MemoryUtil.memFree(buffer);
    }

    /**
     * Samples the noise texture at the given integer coordinates {@code x, y} and returns a normalized float value in the range {@code [0, 1]}<br>
     * The coordinates are wrapped around the texture dimensions to allow for seamless tiling
     * @param x The x coordinate to sample
     * @param y The y coordinate to sample
     * @return A normalized float value in the range {@code [0, 1]} representing the noise value at the given coordinates
     * @see #sample(float, float)
     */
    public float sample(int x, int y) {
        //wrap coordinates
        int px = Maths.modulo(x, width);
        int py = Maths.modulo(y, height);

        //get byte value and normalize to [0, 1]
        int value = buffer.get(px + py * width) & 0xFF;
        return value / 255f;
    }

    /**
     * Samples the noise texture at the given integer coordinates {@code x, y, z} and returns a normalized float value in the range {@code [0, 1]}<br>
     * The coordinates are wrapped around the texture dimensions to allow for seamless tiling
     * @param x The x coordinate to sample
     * @param y The y coordinate to sample
     * @param z The z coordinate to sample
     * @return A normalized float value in the range {@code [0, 1]} representing the noise value at the given coordinates
     * @see #sample(float, float, float)
     */
    public float sample(int x, int y, int z) {
        int px = Maths.modulo(x, width);
        int py = Maths.modulo(y, height);
        int pz = Maths.modulo(z, depth);

        int value = buffer.get(px + py * width + pz * width * height) & 0xFF;
        return value / 255f;
    }

    /**
     * Samples the noise texture at the given floating-point coordinates {@code x, y} and returns a normalized float value in the range {@code [0, 1]}<br>
     * The coordinates are wrapped around the texture dimensions to allow for seamless tiling<br>
     * The value is computed using bilinear interpolation of the four nearest integer samples
     * @param x The x coordinate to sample
     * @param y The y coordinate to sample
     * @return A normalized float value in the range {@code [0, 1]} representing the noise value at the given coordinates
     * @see #sample(int, int)
     */
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

    /**
     * Samples the noise texture at the given floating-point coordinates {@code x, y, z} and returns a normalized float value in the range {@code [0, 1]}<br>
     * The coordinates are wrapped around the texture dimensions to allow for seamless tiling<br>
     * The value is computed using trilinear interpolation of the eight nearest integer samples
     * @param x The x coordinate to sample
     * @param y The y coordinate to sample
     * @param z The z coordinate to sample
     * @return A normalized float value in the range {@code [0, 1]} representing the noise value at the given coordinates
     * @see #sample(int, int, int)
     */
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

    /**
     * Get the width of the noise texture
     * @return The width of the noise texture
     */
    public int getWidth() {
        return width;
    }

    /**
     * Get the height of the noise texture
     * @return The height of the noise texture
     */
    public int getHeight() {
        return height;
    }

    /**
     * Get the depth of the noise texture
     * @return The depth of the noise texture
     */
    public int getDepth() {
        return depth;
    }

    /**
     * Get the seed used for generating the noise texture
     * @return The seed used for generating the noise texture
     */
    public long getSeed() {
        return seed;
    }

    /**
     * Get the {@link ByteBuffer} containing the noise texture data
     * @return The {@link ByteBuffer} containing the noise texture data
     */
    public ByteBuffer getBuffer() {
        return buffer;
    }
}
