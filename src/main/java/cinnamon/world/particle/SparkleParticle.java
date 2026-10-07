package cinnamon.world.particle;

import cinnamon.registry.ParticlesRegistry;

public class SparkleParticle extends SpriteParticle {

    public SparkleParticle(int lifetime, int color) {
        super(ParticlesRegistry.SPARKLE.texture, lifetime, color);
        setEmissive(true);
    }

    @Override
    public ParticlesRegistry getType() {
        return ParticlesRegistry.SPARKLE;
    }
}
