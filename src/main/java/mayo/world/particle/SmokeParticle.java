package mayo.world.particle;

import mayo.render.Texture;
import mayo.utils.Resource;

public class SmokeParticle extends SpriteParticle {

    private static final Texture TEXTURE = Texture.of(new Resource("textures/particles/smoke.png"), 5, 1);

    public SmokeParticle(int lifetime, int color) {
        super(TEXTURE, lifetime, color);
    }
}
