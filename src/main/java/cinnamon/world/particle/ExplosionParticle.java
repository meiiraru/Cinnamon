package cinnamon.world.particle;

import cinnamon.registry.ParticlesRegistry;
import org.joml.Vector3f;

public class ExplosionParticle extends SpriteParticle {

    private static final Vector3f DEFAULT_MOTION = new Vector3f(0, 0.01f, 0);

    public ExplosionParticle(int lifetime) {
        super(ParticlesRegistry.EXPLOSION.texture, lifetime, 0xFFFFFFFF);
        setEmissive(true);
        setMotion(DEFAULT_MOTION);
    }

    @Override
    public ParticlesRegistry getType() {
        return ParticlesRegistry.EXPLOSION;
    }
}
