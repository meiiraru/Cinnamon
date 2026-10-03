package cinnamon.world.particle;

import cinnamon.registry.ParticlesRegistry;
import cinnamon.render.Camera;
import cinnamon.render.MatrixStack;
import org.joml.Math;

public class LeafParticle extends SpriteParticle {

    public LeafParticle(int lifetime, int color) {
        super(ParticlesRegistry.LEAF.texture, lifetime, color);
        this.setMotion(0.1f, -0.1f, 0.1f);
    }

    @Override
    public void tick() {
        super.tick();
        if (collideTerrain())
            getMotion().zero();
    }

    @Override
    protected void renderParticle(Camera camera, MatrixStack matrices, float delta) {
        float age = getAge() + delta;
        int lifetime = getLifetime();
        int delay = 10;
        if (age >= lifetime - delay) {
            float d = (age - (lifetime - delay)) / delay;
            setScale(Math.lerp(getScale(), 0, d));
        }

        super.renderParticle(camera, matrices, delta);
    }

    @Override
    public ParticlesRegistry getType() {
        return ParticlesRegistry.LEAF;
    }
}
