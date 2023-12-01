package mayo.world.particle;

import mayo.registry.ParticlesRegistry;

public class BrokenHeartParticle extends SpriteParticle {

    public BrokenHeartParticle(int lifetime, int color) {
        super(lifetime, color);
    }

    @Override
    public ParticlesRegistry getType() {
        return ParticlesRegistry.BROKEN_HEARTH;
    }
}
