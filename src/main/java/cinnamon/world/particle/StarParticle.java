package cinnamon.world.particle;

import cinnamon.registry.ParticlesRegistry;
import org.joml.Vector3f;

public class StarParticle extends SpriteParticle {

    private static final Vector3f DEFAULT_MOTION = new Vector3f(0, 0.03f, 0);

    public StarParticle(int lifetime, int color) {
        super(lifetime, color);
        this.setMotion(DEFAULT_MOTION);
    }

    @Override
    public void tick() {
        super.tick();
    }

    @Override
    public ParticlesRegistry getType() {
        return ParticlesRegistry.STAR;
    }
}
