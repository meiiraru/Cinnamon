package cinnamon.world.particle;

import cinnamon.registry.ParticlesRegistry;
import cinnamon.utils.PerlinNoise;
import org.joml.Math;
import org.joml.Vector3f;

public class LightParticle extends SpriteParticle {

    private static final PerlinNoise NOISE = new PerlinNoise();

    private final long seed;
    private final Vector3f lastNoise = new Vector3f();

    private float speed = 1f;

    public LightParticle(int lifetime, int color) {
        super(ParticlesRegistry.LIGHT.texture, lifetime, color);
        this.setEmissive(true);
        this.seed = (int) (Math.random() * (~0L >> 32));
    }

    @Override
    public void tick() {
        super.tick();

        this.pos.sub(lastNoise);

        float scale = 1000f;
        int age = (int) (this.age + seed);

        float nx = NOISE.sample((int) (this.pos.x * scale) + age, (int) (this.pos.y * scale) + age);
        float ny = NOISE.sample((int) (this.pos.y * scale) + age, (int) (this.pos.z * scale) + age);
        float nz = NOISE.sample((int) (this.pos.z * scale) + age, (int) (this.pos.x * scale) + age);

        lastNoise.set(nx - 0.5f, ny - 0.5f, nz - 0.5f).mul(2f * speed);
        this.pos.add(lastNoise);
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
