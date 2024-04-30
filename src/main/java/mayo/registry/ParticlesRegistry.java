package mayo.registry;

import mayo.render.texture.Texture;
import mayo.utils.Resource;

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

    public final Resource resource;
    private final int hFrames, vFrames;
    private Texture texture;

    ParticlesRegistry(String texture) {
        this(texture, 1, 1);
    }

    ParticlesRegistry(String texture, int hFrames, int vFrames) {
        this.resource = texture == null ? null : new Resource(texture);
        this.hFrames = hFrames;
        this.vFrames = vFrames;
    }

    public void loadTexture() {
        this.texture = Texture.of(resource, hFrames, vFrames);
    }

    public Texture getTexture() {
        return texture;
    }

    public static void loadAllTextures() {
        for (ParticlesRegistry particle : values())
            particle.loadTexture();
    }
}
