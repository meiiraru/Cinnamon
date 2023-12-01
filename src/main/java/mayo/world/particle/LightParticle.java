package mayo.world.particle;

import mayo.Client;
import mayo.registry.ParticlesRegistry;
import mayo.utils.Maths;
import mayo.utils.PerlinNoise;

public class LightParticle extends SpriteParticle {

    private static final PerlinNoise NOISE = new PerlinNoise();

    private final int seed;
    private float speed = 1f;

    public LightParticle(int lifetime, int color) {
        super(lifetime, color);
        this.setEmissive(true);
        this.seed = Client.getInstance().ticks;
    }

    @Override
    public void tick() {
        super.tick();
        float x = getAge() * 0.001f + seed;
        this.move(Maths.rotToDir(NOISE.noise(x) * 360, NOISE.noise(x + 42) * 360).mul(0.01f * speed));
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
