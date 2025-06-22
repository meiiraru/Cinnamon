package cinnamon.world.particle;

import cinnamon.registry.ParticlesRegistry;

public class FireParticle extends SpriteParticle {

    public FireParticle(int lifetime) {
        super(ParticlesRegistry.FIRE.texture, lifetime, 0xFFFFFFFF);
        setEmissive(true);
    }

    @Override
    public int getCurrentFrame() {
        return getAge() % getFrameCount();
    }

    @Override
    public ParticlesRegistry getType() {
        return ParticlesRegistry.FIRE;
    }
}
