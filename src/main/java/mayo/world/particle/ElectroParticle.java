package mayo.world.particle;

import mayo.render.Texture;
import mayo.utils.Resource;
import mayo.world.World;

public class ElectroParticle extends SpriteParticle {

    private static final Texture TEXTURE = Texture.of(new Resource("textures/particles/electro.png"), 4, 1);

    public ElectroParticle(World world, int lifetime, int color) {
        super(TEXTURE, world, lifetime, color);
    }
}
