package cinnamon.utils;

import org.joml.Math;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.Random;

/**
 * 2D tileable Perlin noise generator
 */
public class PerlinNoise {

    private final int width, height;
    private final long seed;
    private final int cells;
    private final ByteBuffer buffer;

    public PerlinNoise() {
        this(256, 256, System.nanoTime(), 8);
    }

    public PerlinNoise(int width, int height, long seed, int cells) {
        this.width = width;
        this.height = height;
        this.seed = seed;
        this.cells = cells;
        this.buffer = MemoryUtil.memAlloc(width * height);
        computeNoise(seed, cells);
    }

    private void computeNoise(long seed, int cells) {
        Random random = new Random(seed);

        //generate random gradient vectors for each grid point
        //cells + 1 for tiling
        float[][] gradX = new float[cells + 1][cells + 1];
        float[][] gradY = new float[cells + 1][cells + 1];

        for (int y = 0; y <= cells; y++) {
            for (int x = 0; x <= cells; x++) {
                double angle = random.nextDouble() * Math.PI * 2;
                gradX[y][x] = (float) Math.cos(angle);
                gradY[y][x] = (float) Math.sin(angle);
            }
        }

        //make it tileable by copying edges
        for (int i = 0; i <= cells; i++) {
            gradX[i][cells] = gradX[i][0];
            gradY[i][cells] = gradY[i][0];
            gradX[cells][i] = gradX[0][i];
            gradY[cells][i] = gradY[0][i];
        }

        //fill the noise buffer
        for (int py = 0; py < height; py++) {
            for (int px = 0; px < width; px++) {
                //compute noise value at this pixel
                float noise = computePerlinNoise(px, py, width, height, cells, gradX, gradY);

                //normalize from [-1, 1] to [0, 255]
                int value = (int) ((noise * 0.5f + 0.5f) * 0xFF);
                value = Maths.clamp(value, 0, 0xFF);

                //store as byte
                buffer.put((byte) value);
            }
        }

        buffer.flip();
    }

    private static float computePerlinNoise(int px, int py, int width, int height, int cells, float[][] gradX, float[][] gradY) {
        //scale to grid space
        float x = (float) px / width  * cells;
        float y = (float) py / height * cells;

        //grid cell coordinates
        int x0 = (int) Math.floor(x) % cells;
        int y0 = (int) Math.floor(y) % cells;
        int x1 = x0 + 1;
        int y1 = y0 + 1;

        //fractional position within cell
        float fx = x - Math.floor(x);
        float fy = y - Math.floor(y);

        //quintic interpolation
        float u = fx * fx * fx * (fx * (fx * 6 - 15) + 10);
        float v = fy * fy * fy * (fy * (fy * 6 - 15) + 10);

        //compute dot product with the gradient vectors
        float n00 = gradX[y0][x0] * fx       + gradY[y0][x0] * fy;
        float n10 = gradX[y0][x1] * (fx - 1) + gradY[y0][x1] * fy;
        float n01 = gradX[y1][x0] * fx       + gradY[y1][x0] * (fy - 1);
        float n11 = gradX[y1][x1] * (fx - 1) + gradY[y1][x1] * (fy - 1);

        //bilinear interpolation
        float nx0 = Math.lerp(n00, n10, u);
        float nx1 = Math.lerp(n01, n11, u);
        return Math.lerp(nx0, nx1, v);
    }

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

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public long getSeed() {
        return seed;
    }

    public int getCells() {
        return cells;
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }
}