package mayo.world.particle;

import mayo.render.Texture;
import mayo.utils.Resource;
import org.joml.Vector3f;

public class CloudParticle extends SpriteParticle {

    private static final Texture TEXTURE = Texture.of(new Resource("textures/particles/cloud.png"), 5, 1);
    private static final Vector3f DEFAULT_MOTION = new Vector3f(0, 0.01f, 0);

    public CloudParticle(int lifetime, int color) {
        super(TEXTURE, lifetime, color);
        setMotion(DEFAULT_MOTION);
    }
}
