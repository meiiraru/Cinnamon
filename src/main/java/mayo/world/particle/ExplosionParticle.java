package mayo.world.particle;

import mayo.render.Texture;
import mayo.utils.Resource;
import mayo.world.World;
import org.joml.Vector3f;

public class ExplosionParticle extends SpriteParticle {

    private static final Texture TEXTURE = Texture.of(new Resource("textures/particles/explosion.png"), 6, 1);
    private static final Vector3f DEFAULT_MOTION = new Vector3f(0, 0.01f, 0);

    public ExplosionParticle(World world, int lifetime) {
        super(TEXTURE, world, lifetime, -1);
        setMotion(DEFAULT_MOTION);
    }
}
