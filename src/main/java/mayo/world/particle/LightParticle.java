package mayo.world.particle;

import mayo.render.Texture;
import mayo.utils.Resource;
import org.joml.Vector3f;

public class LightParticle extends SpriteParticle {

    private static final Texture TEXTURE = Texture.of(new Resource("textures/particles/light.png"), 4, 1);

    public LightParticle(int lifetime, int color) {
        super(TEXTURE, lifetime, color);
    }

    @Override
    public void tick() {
        super.tick();

        Vector3f motion = this.getMotion();
        if (Math.random() < 0.5f)
            motion.mul(-1);

        this.setMotion(motion.y, motion.x, motion.z);
    }
}
