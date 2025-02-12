package cinnamon.world.particle;

import cinnamon.registry.ParticlesRegistry;
import cinnamon.sound.SoundCategory;
import cinnamon.utils.AABB;
import cinnamon.utils.Maths;
import cinnamon.utils.Resource;

public class BubbleParticle extends SpriteParticle {

    private static final Resource POP_SOUND = new Resource("sounds/particle/bubble/pop.ogg");

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
        return Maths.clamp(getAge() - getLifetime() + frames, 0, frames);
    }

    @Override
    public void remove() {
        super.remove();
        world.playSound(POP_SOUND, SoundCategory.AMBIENT, pos).volume(0.03f).pitch(Maths.range(0.8f, 1.2f)).distance(0f).maxDistance(8f);
    }

    @Override
    public ParticlesRegistry getType() {
        return ParticlesRegistry.BUBBLE;
    }
}
