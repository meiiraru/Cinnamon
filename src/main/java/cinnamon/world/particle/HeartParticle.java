package cinnamon.world.particle;

import cinnamon.registry.ParticlesRegistry;

public class HeartParticle extends SpriteParticle {

    public HeartParticle(int lifetime, int color) {
        super(ParticlesRegistry.HEARTH.texture, lifetime, color);
    }

    @Override
    public ParticlesRegistry getType() {
        return ParticlesRegistry.HEARTH;
    }
}
