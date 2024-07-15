package cinnamon.world.particle;

import cinnamon.registry.ParticlesRegistry;
import org.joml.Vector3f;

public class SmokeParticle extends SpriteParticle {

    private static final Vector3f DEFAULT_MOTION = new Vector3f(0, 0.01f, 0);

    public SmokeParticle(int lifetime, int color) {
        super(lifetime, color);
        setMotion(DEFAULT_MOTION);
    }

    @Override
    public ParticlesRegistry getType() {
        return ParticlesRegistry.SMOKE;
    }
}
