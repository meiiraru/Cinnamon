package mayo.world.particle;

import mayo.render.Texture;
import mayo.utils.Resource;
import mayo.world.World;

public class BrokenHeartParticle extends SpriteParticle {

    private static final Texture TEXTURE = Texture.of(new Resource("textures/particles/broken_heart.png"));

    public BrokenHeartParticle(World world, int lifetime, int color) {
        super(TEXTURE, world, lifetime, color);
    }
}
