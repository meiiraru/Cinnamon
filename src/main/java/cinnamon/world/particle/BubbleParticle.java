package cinnamon.world.particle;

import cinnamon.registry.ParticlesRegistry;
import cinnamon.sound.SoundCategory;
import cinnamon.utils.AABB;
import cinnamon.utils.Maths;
import cinnamon.utils.Resource;
import cinnamon.world.entity.Entity;
import cinnamon.world.entity.PhysEntity;

public class BubbleParticle extends SpriteParticle {

    private static final Resource POP_SOUND = new Resource("sounds/particle/bubble/pop.ogg");

    public BubbleParticle(int lifetime, int color) {
        super(ParticlesRegistry.BUBBLE.texture, lifetime, color);
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
                    break;
                }
            }

            if (!collided) {
                for (Entity entity : world.getEntities(aabb)) {
                    if (entity instanceof PhysEntity && aabb.intersects(entity.getAABB())) {
                        getMotion().zero();
                        collided = true;
                        break;
                    }
                }
            }

            if (collided)
                age = lifetime - (getFrameCount() - 1);
        }
    }

    @Override
    public int getCurrentFrame() {
        int frames = getFrameCount() - 1;
        return Maths.clamp(getAge() - getLifetime() + frames, 0, frames);
    }

    @Override
    public void remove() {
        super.remove();
        world.playSound(POP_SOUND, SoundCategory.AMBIENT, pos).volume(0.5f).pitch(Maths.range(0.8f, 1.2f)).distance(0f).maxDistance(8f);
    }

    @Override
    public ParticlesRegistry getType() {
        return ParticlesRegistry.BUBBLE;
    }
}
