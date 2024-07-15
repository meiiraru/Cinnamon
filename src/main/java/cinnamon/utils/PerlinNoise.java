package cinnamon.utils;

import java.util.Random;

public class PerlinNoise {
    private final int[] permutation;

    public PerlinNoise() {
        permutation = new int[512];
        Random rand = new Random();

        for (int i = 0; i < 256; i++) {
            permutation[i] = i;
        }

        for (int i = 0; i < 256; i++) {
            int j = rand.nextInt(256 - i) + i;
            int temp = permutation[i];
            permutation[i] = permutation[j];
            permutation[j] = temp;
            permutation[i + 256] = permutation[i];
        }
    }

    private float fade(float t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    private float grad(int hash, float x) {
        int h = hash & 15;
        float grad = 1f + (h & 7); //gradient value 1 to 8
        if ((h & 8) != 0) grad = -grad; //randomly invert half of them
        return (grad * x);
    }

    public float noise(float x) {
        int X = (int) Math.floor(x) & 255;
        x -= (float) Math.floor(x);
        float u = fade(x);
        return Maths.lerp(u, grad(permutation[X], x), grad(permutation[X + 1], x - 1)) * 2;
    }
}
