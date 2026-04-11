package cinnamon.math.noise;

import cinnamon.math.Maths;
import org.joml.Math;

import java.util.Random;

/**
 * 2D tileable Voronoi noise generator
 */
public class VoronoiNoise2D extends Noise {

    public static int DEFAULT_CELLS = 8;
    public static float DEFAULT_JITTER = 1f;

    protected final int cells;
    protected final float jitter;

    public VoronoiNoise2D() {
        this(DEFAULT_SIZE, DEFAULT_SIZE, System.nanoTime(), DEFAULT_CELLS, DEFAULT_JITTER);
    }

    public VoronoiNoise2D(int width, int height, long seed) {
        this(width, height, seed, DEFAULT_CELLS, DEFAULT_JITTER);
    }

    public VoronoiNoise2D(int width, int height, long seed, int cells, float jitter) {
        super(width, height, seed);
        this.cells = Math.max(1, cells);
        this.jitter = Maths.clamp(jitter, 0f, 1f);
        this.build();
    }

    @Override
    protected void build() {
        Random random = new Random(seed);

        //one feature point per cell with controllable jitter around the center
        float[][] pointsX = new float[cells][cells];
        float[][] pointsY = new float[cells][cells];
        float halfJitter = jitter * 0.5f;

        for (int y = 0; y < cells; y++) {
            for (int x = 0; x < cells; x++) {
                pointsX[y][x] = 0.5f + (random.nextFloat() - 0.5f) * jitter;
                pointsY[y][x] = 0.5f + (random.nextFloat() - 0.5f) * jitter;

                //safety clamp for any edge-case floating-point drift
                pointsX[y][x] = Maths.clamp(pointsX[y][x], 0.5f - halfJitter, 0.5f + halfJitter);
                pointsY[y][x] = Maths.clamp(pointsY[y][x], 0.5f - halfJitter, 0.5f + halfJitter);
            }
        }

        final float maxDistance = Math.sqrt(2f);

        for (int py = 0; py < height; py++) {
            for (int px = 0; px < width; px++) {
                float x = (float) px / width * cells;
                float y = (float) py / height * cells;

                int cx = (int) Math.floor(x);
                int cy = (int) Math.floor(y);

                float minDistanceSq = Float.MAX_VALUE;

                //check the current and neighbor cells for the nearest feature point
                for (int oy = -1; oy <= 1; oy++) {
                    int ny = cy + oy;
                    int wy = Maths.modulo(ny, cells);

                    for (int ox = -1; ox <= 1; ox++) {
                        int nx = cx + ox;
                        int wx = Maths.modulo(nx, cells);

                        float fx = nx + pointsX[wy][wx];
                        float fy = ny + pointsY[wy][wx];

                        float dx = fx - x;
                        float dy = fy - y;
                        float distanceSq = dx * dx + dy * dy;

                        if (distanceSq < minDistanceSq)
                            minDistanceSq = distanceSq;
                    }
                }

                float distance = Math.sqrt(minDistanceSq);
                float normalized = 1f - Maths.clamp(distance / maxDistance, 0f, 1f);

                int value = (int) (normalized * 0xFF);
                value = Maths.clamp(value, 0, 0xFF);
                buffer.put((byte) value);
            }
        }

        buffer.flip();
    }

    public int getCells() {
        return cells;
    }

    public float getJitter() {
        return jitter;
    }
}

