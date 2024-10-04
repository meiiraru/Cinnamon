package cinnamon.world.particle;

import cinnamon.registry.ParticlesRegistry;

public class BubbleParticle extends SpriteParticle {

    public BubbleParticle(int lifetime, int color) {
        super(lifetime, color);
    }

    @Override
    public void tick() {
        getMotion().mul(0.99f, 1f, 0.99f);
        super.tick();
    }

    @Override
    public int getCurrentFrame() {
        int frames = texture.getUFrames() - 1;
        return Math.clamp(getAge() - getLifetime() + frames, 0, frames);
    }

    @Override
    public ParticlesRegistry getType() {
        return ParticlesRegistry.BUBBLE;
    }
}
