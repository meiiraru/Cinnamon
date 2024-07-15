package cinnamon.world.particle;

import cinnamon.registry.ParticlesRegistry;

public class HeartParticle extends SpriteParticle {

    public HeartParticle(int lifetime, int color) {
        super(lifetime, color);
    }

    @Override
    public ParticlesRegistry getType() {
        return ParticlesRegistry.HEARTH;
    }
}
