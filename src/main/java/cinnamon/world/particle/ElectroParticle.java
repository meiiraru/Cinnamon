package cinnamon.world.particle;

import cinnamon.registry.ParticlesRegistry;

public class ElectroParticle extends SpriteParticle {

    public ElectroParticle(int lifetime, int color) {
        super(lifetime, color);
    }

    @Override
    public ParticlesRegistry getType() {
        return ParticlesRegistry.ELECTRO;
    }
}
