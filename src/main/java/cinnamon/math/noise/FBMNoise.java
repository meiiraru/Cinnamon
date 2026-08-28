package cinnamon.math.noise;

import cinnamon.math.Maths;

public class FBMNoise extends Noise {

    public static final float
            DEFAULT_LACUNARITY = 2f,
            DEFAULT_GAIN       = 0.5f,
            DEFAULT_AMPLITUDE  = 0.5f,
            DEFAULT_FREQUENCY  = 1f;

    private final Noise baseNoise;
    private final int octaves;
    private final float lacunarity, gain, amplitude, frequency;

    public FBMNoise(Noise other, int octaves) {
        this(other, octaves, DEFAULT_LACUNARITY, DEFAULT_GAIN, DEFAULT_AMPLITUDE, DEFAULT_FREQUENCY);
    }

    public FBMNoise(Noise other, int octaves, float lacunarity, float gain, float amplitude, float frequency) {
        super(other.getWidth(), other.getHeight(), other.getDepth(), other.getSeed());
        this.baseNoise = other;
        this.octaves = octaves;
        this.lacunarity = lacunarity;
        this.gain = gain;
        this.amplitude = amplitude;
        this.frequency = frequency;
        this.build();
    }

    @Override
    protected void build() {
        for (int pz = 0; pz < depth; pz++) {
            for (int py = 0; py < height; py++) {
                for (int px = 0; px < width; px++) {

                    float value = 0f;
                    float frequency = this.frequency;
                    float amplitude = this.amplitude;

                    //standard Fractal Brownian Motion loop
                    for (int i = 0; i < octaves; i++) {
                        float noiseVal;

                        if (depth > 1) {
                            //3D sampling
                            noiseVal = baseNoise.sample(px * frequency, py * frequency, pz * frequency);
                        } else {
                            //2D sampling
                            noiseVal = baseNoise.sample(px * frequency, py * frequency);
                        }

                        //convert base noise from [0, 1] to [-1, 1]
                        noiseVal = noiseVal * 2f - 1f;

                        value += noiseVal * amplitude;
                        frequency *= lacunarity;
                        amplitude *= gain;
                    }

                    //convert final value from [-1, 1] back to [0, 255]
                    int byteValue = (int) ((value * 0.5f + 0.5f) * 0xFF);
                    byteValue = Maths.clamp(byteValue, 0, 0xFF);

                    buffer.put((byte) byteValue);
                }
            }
        }

        buffer.flip();
    }

    public int getOctaves() {
        return octaves;
    }

    public float getLacunarity() {
        return lacunarity;
    }

    public float getGain() {
        return gain;
    }

    public float getAmplitude() {
        return amplitude;
    }

    public float getFrequency() {
        return frequency;
    }
}
