package cinnamon.math.noise;

import java.util.Random;

public class WhiteNoise2D extends Noise {

    public WhiteNoise2D() {
        this(DEFAULT_SIZE, DEFAULT_SIZE, System.nanoTime());
    }

    public WhiteNoise2D(int width, int height, long seed) {
        super(width, height, seed);
        this.build();
    }

    @Override
    protected void build() {
        Random random = new Random(seed);
        for (int i = 0; i < buffer.capacity(); i++)
            buffer.put(i, (byte) (random.nextFloat() * 255f));
    }
}
