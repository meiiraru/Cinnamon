package cinnamon.math.noise;

import java.util.Random;

/**
 * 2D "tileable" white noise generator
 */
public class WhiteNoise2D extends Noise {

    /**
     * Creates a new white noise with the default size and a random seed
     * @see #WhiteNoise2D(int, int, long)
     */
    public WhiteNoise2D() {
        this(DEFAULT_SIZE, DEFAULT_SIZE, System.nanoTime());
    }

    /**
     * Creates a new white noise
     * @param width The width of the noise texture
     * @param height The height of the noise texture
     * @param seed The seed for the noise generation
     * @see #WhiteNoise2D()
     */
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
