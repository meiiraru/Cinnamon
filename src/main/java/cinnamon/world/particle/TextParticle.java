package cinnamon.world.particle;

import cinnamon.registry.ParticlesRegistry;
import cinnamon.render.Camera;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.text.Text;
import cinnamon.utils.Alignment;
import cinnamon.world.world.WorldClient;
import org.joml.Vector3f;

public class TextParticle extends Particle {

    private static final Vector3f MOTION = new Vector3f(0f, 0.05f, 0f);

    private final Text text;

    public TextParticle(Text text, int lifetime, Vector3f position) {
        super(lifetime);
        this.text = text;
        this.setPos(position);
        this.setMotion(MOTION);
    }

    @Override
    protected void renderParticle(MatrixStack matrices, float delta) {
        matrices.scale(-PARTICLE_SCALING);
        text.render(isEmissive() ? VertexConsumer.FONT : VertexConsumer.WORLD_FONT, matrices, 0, 0, Alignment.TOP_CENTER);
    }

    @Override
    public boolean shouldRender(Camera camera) {
        return !((WorldClient) world).hideHUD() && super.shouldRender(camera);
    }

    @Override
    protected int getRenderDistance() {
        return 256;
    }

    @Override
    public ParticlesRegistry getType() {
        return ParticlesRegistry.TEXT;
    }
}
