package cinnamon.world.particle;

import cinnamon.registry.ParticlesRegistry;

public class SteamParticle extends SpriteParticle {

    public SteamParticle(int lifetime, int color) {
        super(ParticlesRegistry.STEAM.texture, lifetime, color);
    }

    @Override
    public ParticlesRegistry getType() {
        return ParticlesRegistry.STEAM;
    }
}
