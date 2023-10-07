package mayo.world.particle;

import mayo.Client;
import mayo.render.Texture;
import mayo.utils.Maths;
import mayo.utils.PerlinNoise;
import mayo.utils.Resource;

public class LightParticle extends SpriteParticle {

    private static final Texture TEXTURE = Texture.of(new Resource("textures/particles/light.png"), 4, 1);
    private static final PerlinNoise NOISE = new PerlinNoise();

    private final int seed;
    private float speed = 1f;

    public LightParticle(int lifetime, int color) {
        super(TEXTURE, lifetime, color);
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
}
