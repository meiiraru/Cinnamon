package mayo.world.particle;

import mayo.render.Texture;
import mayo.render.batch.VertexConsumer;
import mayo.utils.Resource;
import mayo.world.World;

public class SquareParticle extends SpriteParticle {

    private static final Texture TEXTURE = Texture.of(new Resource("textures/particles/square.png"));

    private final boolean emissive;

    public SquareParticle(World world, int lifetime, int color, boolean emissive) {
        super(TEXTURE, world, lifetime, color);
        this.emissive = emissive;
    }

    @Override
    protected VertexConsumer vertexConsumer() {
        return emissive ? VertexConsumer.MAIN_FLAT : super.vertexConsumer();
    }
}
