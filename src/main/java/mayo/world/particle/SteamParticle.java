package mayo.world.particle;

import mayo.render.Texture;
import mayo.utils.Resource;
import mayo.world.World;

public class SteamParticle extends SpriteParticle {

    private static final Texture TEXTURE = Texture.of(new Resource("textures/particles/steam.png"), 5, 1);

    public SteamParticle(World world, int lifetime, int color) {
        super(TEXTURE, world, lifetime, color);
    }
}
