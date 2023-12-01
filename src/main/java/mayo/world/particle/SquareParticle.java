package mayo.world.particle;

import mayo.registry.ParticlesRegistry;

public class SquareParticle extends SpriteParticle {

    public SquareParticle(int lifetime, int color) {
        super(lifetime, color);
    }

    @Override
    public ParticlesRegistry getType() {
        return ParticlesRegistry.SQUARE;
    }
}
