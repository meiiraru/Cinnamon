package cinnamon.world.particle;

import cinnamon.registry.ParticlesRegistry;

public class FireParticle extends SpriteParticle {

    public FireParticle(int lifetime) {
        super(lifetime, 0xFFFFFFFF);
        setEmissive(true);
    }

    @Override
    public int getCurrentFrame() {
        return getAge() % texture.getUFrames();
    }

    @Override
    public ParticlesRegistry getType() {
        return ParticlesRegistry.FIRE;
    }
}
