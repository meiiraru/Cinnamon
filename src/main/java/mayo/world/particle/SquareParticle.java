package mayo.world.particle;

import mayo.render.Texture;
import mayo.utils.Resource;

public class SquareParticle extends SpriteParticle {

    private static final Texture TEXTURE = Texture.of(new Resource("textures/particles/square.png"));

    public SquareParticle(int lifetime, int color) {
        super(TEXTURE, lifetime, color);
    }
}
