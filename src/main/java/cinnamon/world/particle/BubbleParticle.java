package cinnamon.world.particle;

import cinnamon.registry.ParticlesRegistry;
import cinnamon.utils.AABB;

public class BubbleParticle extends SpriteParticle {

    public BubbleParticle(int lifetime, int color) {
        super(lifetime, color);
    }

    private boolean collided;

    @Override
    public void tick() {
        getMotion().mul(0.99f, 1f, 0.99f);
        super.tick();

        if (!collided) {
            AABB aabb = getAABB();
            for (AABB terrain : world.getTerrainCollisions(aabb)) {
                if (aabb.intersects(terrain)) {
                    getMotion().zero();
                    collided = true;
                    age = lifetime - (texture.getUFrames() - 1);
                }
            }
        }
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
