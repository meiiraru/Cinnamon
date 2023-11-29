package mayo.world.particle;

import mayo.render.Texture;
import mayo.utils.Resource;

public class HeartParticle extends SpriteParticle {

    private static final Texture TEXTURE = Texture.of(new Resource("textures/particles/heart.png"));

    public HeartParticle(int lifetime, int color) {
        super(TEXTURE, lifetime, color);
    }
}
