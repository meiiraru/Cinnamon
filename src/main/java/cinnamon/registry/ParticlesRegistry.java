package cinnamon.registry;

import cinnamon.utils.Resource;

public enum ParticlesRegistry {
    BROKEN_HEARTH(new Resource("textures/particles/broken_heart.png")),
    BUBBLE(new Resource("textures/particles/bubble.png")),
    CONFETTI(new Resource("textures/particles/confetti.png")),
    DUST(new Resource("textures/particles/dust.png")),
    ELECTRO(new Resource("textures/particles/electro.png")),
    EXPLOSION(new Resource("textures/particles/explosion.png")),
    FIRE(new Resource("textures/particles/fire.png")),
    HEARTH(new Resource("textures/particles/heart.png")),
    LIGHT(new Resource("textures/particles/light.png")),
    SMOKE(new Resource("textures/particles/smoke.png")),
    SQUARE(new Resource("textures/particles/square.png")),
    STAR(new Resource("textures/particles/star.png")),
    STEAM(new Resource("textures/particles/steam.png")),
    OTHER(null);

    public final Resource texture;

    ParticlesRegistry(Resource texture) {
        this.texture = texture;
    }
}
