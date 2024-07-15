package cinnamon.world.particle;

import cinnamon.registry.ParticlesRegistry;
import org.joml.Vector3f;

public class DustParticle extends SpriteParticle {

    private static final Vector3f DEFAULT_MOTION = new Vector3f(0, 0.01f, 0);

    public DustParticle(int lifetime, int color) {
        super(lifetime, color);
        this.setMotion(DEFAULT_MOTION);
    }

    @Override
    public ParticlesRegistry getType() {
        return ParticlesRegistry.DUST;
    }
}
