package mayo.world.particle;

import mayo.registry.ParticlesRegistry;

public class HeartParticle extends SpriteParticle {

    public HeartParticle(int lifetime, int color) {
        super(lifetime, color);
    }

    @Override
    public ParticlesRegistry getType() {
        return ParticlesRegistry.HEARTH;
    }
}
