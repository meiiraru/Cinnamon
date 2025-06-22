package cinnamon.world.particle;

import cinnamon.registry.ParticlesRegistry;

public class ConfettiParticle extends SpriteParticle {

    public ConfettiParticle(int lifetime, int color) {
        super(ParticlesRegistry.CONFETTI.texture, lifetime, color);
    }

    @Override
    public int getCurrentFrame() {
        return getAge() % 2;
    }

    @Override
    public ParticlesRegistry getType() {
        return ParticlesRegistry.CONFETTI;
    }
}
