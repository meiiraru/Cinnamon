package mayo.world.particle;

import mayo.render.Texture;
import mayo.utils.Resource;

public class BrokenHeartParticle extends SpriteParticle {

    private static final Texture TEXTURE = Texture.of(new Resource("textures/particles/broken_heart.png"));

    public BrokenHeartParticle(int lifetime, int color) {
        super(TEXTURE, lifetime, color);
    }
}
