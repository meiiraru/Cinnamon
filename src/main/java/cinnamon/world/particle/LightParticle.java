package cinnamon.world.particle;

import cinnamon.Client;
import cinnamon.registry.ParticlesRegistry;
import cinnamon.utils.Maths;
import cinnamon.utils.PerlinNoise;

public class LightParticle extends SpriteParticle {

    private static final PerlinNoise NOISE = new PerlinNoise();

    private final int seed;
    private float speed = 1f;

    public LightParticle(int lifetime, int color) {
        super(ParticlesRegistry.LIGHT.texture, lifetime, color);
        this.setEmissive(true);
        this.seed = (int) (Client.getInstance().ticks & (~0L >> 32));
    }

    @Override
    public void tick() {
        super.tick();
        float x = getAge() * 0.001f + seed;
        this.move(Maths.rotToDir(NOISE.sample(x, 0) * 360, NOISE.sample(0, x + 42) * 360).mul(0.01f * speed));
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public float getSpeed() {
        return speed;
    }

    @Override
    public ParticlesRegistry getType() {
        return ParticlesRegistry.LIGHT;
    }
}
