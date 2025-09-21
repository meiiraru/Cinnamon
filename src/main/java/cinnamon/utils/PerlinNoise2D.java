package cinnamon.utils;

import java.util.Random;

/**
 * Simple seeded 2D Perlin noise for terrain heightmaps.
 * Deterministic given the same seed.
 */
public class PerlinNoise2D {

    private final int[] p = new int[512];

    public PerlinNoise2D(long seed) {
        int[] perm = new int[256];
        for (int i = 0; i < 256; i++) perm[i] = i;
        Random rand = new Random(seed);
        for (int i = 255; i > 0; i--) {
            int j = rand.nextInt(i + 1);
            int tmp = perm[i];
            perm[i] = perm[j];
            perm[j] = tmp;
        }
        for (int i = 0; i < 512; i++) p[i] = perm[i & 255];
    }

    private static float fade(float t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    private static float lerp(float t, float a, float b) {
        return a + t * (b - a);
    }

    private static float grad(int hash, float x, float y) {
        int h = hash & 7; // 8 gradients
        float u = (h < 4) ? x : y;
        float v = (h < 4) ? y : x;
        return (((h & 1) == 0) ? u : -u) + (((h & 2) == 0) ? v : -v);
    }

    /**
     * Single octave noise in range roughly [-1, 1].
     */
    public float noise(float x, float y) {
        int X = (int) Math.floor(x) & 255;
        int Y = (int) Math.floor(y) & 255;

        float xf = x - (float) Math.floor(x);
        float yf = y - (float) Math.floor(y);

        float u = fade(xf);
        float v = fade(yf);

        int aa = p[p[X] + Y];
        int ab = p[p[X] + Y + 1];
        int ba = p[p[X + 1] + Y];
        int bb = p[p[X + 1] + Y + 1];

        float x1 = lerp(u, grad(aa, xf, yf), grad(ba, xf - 1, yf));
        float x2 = lerp(u, grad(ab, xf, yf - 1), grad(bb, xf - 1, yf - 1));
        return lerp(v, x1, x2);
    }

    /**
     * Fractal brownian motion with octaves and lacunarity/persistence.
     */
    public float fbm(float x, float y, int octaves, float lacunarity, float persistence) {
        float total = 0f;
        float amplitude = 1f;
        float frequency = 1f;
        float max = 0f;
        for (int i = 0; i < octaves; i++) {
            total += noise(x * frequency, y * frequency) * amplitude;
            max += amplitude;
            amplitude *= persistence;
            frequency *= lacunarity;
        }
        return total / max; // normalize to [-1,1] approx
    }
}
