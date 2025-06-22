package cinnamon.world.particle;

import cinnamon.registry.ParticlesRegistry;

public class SquareParticle extends SpriteParticle {

    public SquareParticle(int lifetime, int color) {
        super(ParticlesRegistry.SQUARE.texture, lifetime, color);
    }

    @Override
    public ParticlesRegistry getType() {
        return ParticlesRegistry.SQUARE;
    }
}
