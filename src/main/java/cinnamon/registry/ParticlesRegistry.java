package cinnamon.registry;

import cinnamon.render.texture.SpriteTexture;
import cinnamon.utils.Resource;

public enum ParticlesRegistry {
    BROKEN_HEARTH("textures/particles/broken_heart.png"),
    DUST("textures/particles/dust.png", 4, 1),
    ELECTRO("textures/particles/electro.png"),
    EXPLOSION("textures/particles/explosion.png", 6, 1),
    HEARTH("textures/particles/heart.png"),
    LIGHT("textures/particles/light.png", 4, 1),
    SMOKE("textures/particles/smoke.png", 5, 1),
    SQUARE("textures/particles/square.png"),
    STEAM("textures/particles/steam.png", 5, 1),
    TEXT(null);

    private final SpriteTexture texture;

    ParticlesRegistry(String texture) {
        this(texture, 1, 1);
    }

    ParticlesRegistry(String texture, int hFrames, int vFrames) {
        this.texture = texture == null ? null : new SpriteTexture(new Resource(texture), hFrames, vFrames);
    }

    public SpriteTexture getTexture() {
        return texture;
    }
}
