package cinnamon.utils;

import java.util.Random;

public class PerlinNoise {

    protected final float[][] gradients;
    protected final int gridSize;

    public PerlinNoise() {
        this(System.currentTimeMillis(), 256);
    }

    public PerlinNoise(long seed, int gridSize) {
        this.gridSize = gridSize;
        this.gradients = new float[gridSize][gridSize * 2];
        Random rand = new Random(seed);

        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                float angle = rand.nextFloat() * 2 * (float) Math.PI;
                gradients[i][j * 2] = (float) Math.cos(angle);
                gradients[i][j * 2 + 1] = (float) Math.sin(angle);
            }
        }
    }

    protected float getInfluenceValue(float x, float y, int Xgrad, int Ygrad) {
        int gradXIndex = Maths.modulo(Xgrad, gridSize);
        int gradYIndex = Maths.modulo(Ygrad, gridSize);
        return (gradients[gradXIndex][gradYIndex * 2] * (x - Xgrad)) +
                (gradients[gradXIndex][gradYIndex * 2 + 1] * (y - Ygrad));
    }

    protected float fade(float t) {
        return t * t * t * (t * (t * 6 - 15) + 10); //6t^5 - 15t^4 + 10t^3
    }

    public float sample(float x, float y) {
        int X0 = (int) Math.floor(x);
        int Y0 = (int) Math.floor(y);
        int X1 = X0 + 1;
        int Y1 = Y0 + 1;

        //determine interpolation weights
        float sx = x - X0;
        float sy = y - Y0;

        //smooth the interpolation weights
        float u = fade(sx);
        float v = fade(sy);

        //get influence values from the four corner gradients
        float topLeftDot = getInfluenceValue(x, y, X0, Y1);
        float topRightDot = getInfluenceValue(x, y, X1, Y1);
        float bottomLeftDot = getInfluenceValue(x, y, X0, Y0);
        float bottomRightDot = getInfluenceValue(x, y, X1, Y0);

        //interpolate between the four values
        return Maths.lerp(
                Maths.lerp(bottomLeftDot, bottomRightDot, u),
                Maths.lerp(topLeftDot, topRightDot, u),
                v);
    }
}