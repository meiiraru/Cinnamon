package cinnamon.math.noise;

import cinnamon.math.Maths;
import org.joml.Math;

import java.util.Random;

/**
 * 2D tileable Blue Noise generator
 * Based on Alister Chowdhury's Bluenoise Generator for Big Textures
 */
public class BlueNoise2D extends Noise {

    public static int DEFAULT_TILE_SIZE = 16;
    public static float DEFAULT_SIGMA = 1.9f;

    protected final int tileSize;
    protected final float sigma;

    public BlueNoise2D() {
        this(DEFAULT_SIZE, DEFAULT_SIZE, System.nanoTime(), DEFAULT_TILE_SIZE, DEFAULT_SIGMA);
    }

    public BlueNoise2D(int width, int height, long seed) {
        this(width, height, seed, DEFAULT_TILE_SIZE, DEFAULT_SIGMA);
    }

    public BlueNoise2D(int width, int height, long seed, int tileSize, float sigma) {
        super(width, height, seed);
        this.tileSize = tileSize;
        this.sigma = sigma;
        this.build();
    }

    @Override
    protected void build() {
        Random random = new Random(seed);

        int tilesX = (width + tileSize - 1) / tileSize;
        int tilesY = (height + tileSize - 1) / tileSize;

        //precompute energy LUT (a radius of 8 captures 99.9% of the Gaussian energy for sigma 1.9)
        int radius = 8;
        float[][] kernel = new float[radius * 2 + 1][radius * 2 + 1];
        for (int y = -radius; y <= radius; y++) {
            for (int x = -radius; x <= radius; x++) {
                kernel[y + radius][x + radius] = (float) Math.exp(-(x * x + y * y) / (2f * sigma * sigma));
            }
        }

        int totalPixels = width * height;
        float[] energy = new float[totalPixels];
        boolean[] filled = new boolean[totalPixels];

        //initialize the energy grid with tiny random values to break distance ties evenly
        for (int i = 0; i < totalPixels; i++) {
            energy[i] = random.nextFloat() * 1e-6f;
        }

        //4 staggered phases for the 2x2 grid to ensure independent neighboring tiles
        int[][] phases = {{0, 0}, {1, 1}, {1, 0}, {0, 1}};

        int numIterations = tileSize * tileSize;

        for (int iter = 0; iter < numIterations; iter++) {
            for (int[] phase : phases) {
                int px = phase[0];
                int py = phase[1];

                for (int ty = py; ty < tilesY; ty += 2) {
                    for (int tx = px; tx < tilesX; tx += 2) {

                        int startX = tx * tileSize;
                        int startY = ty * tileSize;

                        //handle resolutions that are not multiples of tile size
                        int tileW = Math.min(tileSize, width - startX);
                        int tileH = Math.min(tileSize, height - startY);
                        int tilePixels = tileW * tileH;

                        float minEnergy = Float.MAX_VALUE;
                        int bestX = -1;
                        int bestY = -1;

                        //find the void in this specific tile
                        for (int y = 0; y < tileH; y++) {
                            for (int x = 0; x < tileW; x++) {
                                int cx = startX + x;
                                int cy = startY + y;

                                int idx = cx + cy * width;
                                if (!filled[idx]) {
                                    if (energy[idx] < minEnergy) {
                                        minEnergy = energy[idx];
                                        bestX = cx;
                                        bestY = cy;
                                    }
                                }
                            }
                        }

                        //if tile is fully filled, skip
                        if (bestX == -1)
                            continue;

                        int bestIdx = bestX + bestY * width;
                        filled[bestIdx] = true;

                        //calculate the relative iteration in [0..255] range
                        int value = tilePixels > 1 ? (int) (((float) iter / (tilePixels - 1)) * 255f) : 0;
                        value = Maths.clamp(value, 0, 0xFF);
                        buffer.put(bestIdx, (byte) value);

                        //update the energy field for the surrounding pixels iteratively
                        for (int dy = -radius; dy <= radius; dy++) {
                            for (int dx = -radius; dx <= radius; dx++) {
                                int ex = bestX + dx;
                                int ey = bestY + dy;

                                //wrap coordinates
                                ex = Maths.modulo(ex, width);
                                ey = Maths.modulo(ey, height);

                                energy[ex + ey * width] += kernel[dy + radius][dx + radius];
                            }
                        }
                    }
                }
            }
        }
    }

    public int getTileSize() {
        return tileSize;
    }

    public float getSigma() {
        return sigma;
    }
}