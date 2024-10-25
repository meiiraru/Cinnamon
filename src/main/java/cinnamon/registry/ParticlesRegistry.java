package cinnamon.registry;

import cinnamon.render.texture.SpriteTexture;
import cinnamon.utils.Resource;

public enum ParticlesRegistry {
    BROKEN_HEARTH("textures/particles/broken_heart.png"),
    BUBBLE("textures/particles/bubble.png", 3),
    CONFETTI("textures/particles/confetti.png", 2),
    DUST("textures/particles/dust.png", 4),
    ELECTRO("textures/particles/electro.png", 4),
    EXPLOSION("textures/particles/explosion.png", 6),
    FIRE("textures/particles/fire.png", 3),
    HEARTH("textures/particles/heart.png"),
    LIGHT("textures/particles/light.png", 4),
    SMOKE("textures/particles/smoke.png", 5),
    SQUARE("textures/particles/square.png"),
    STAR("textures/particles/star.png", 8),
    STEAM("textures/particles/steam.png", 5),
    TEXT(null);

    private final SpriteTexture texture;

    ParticlesRegistry(String texture) {
        this(texture, 1);
    }

    ParticlesRegistry(String texture, int hFrames) {
        this.texture = texture == null ? null : new SpriteTexture(new Resource(texture), hFrames, 1);
    }

    public SpriteTexture getTexture() {
        return texture;
    }
}
