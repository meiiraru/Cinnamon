package mayo.world.particle;

import mayo.Client;
import mayo.render.Font;
import mayo.render.MatrixStack;
import mayo.render.batch.VertexConsumer;
import mayo.text.Text;
import mayo.utils.TextUtils;
import org.joml.Vector3f;

public class TextParticle extends Particle {

    private static final Vector3f MOTION = new Vector3f(0f, 0.05f, 0f);

    private final Text text;
    private final Font font;

    public TextParticle(Text text, int lifetime, Vector3f position) {
        super(lifetime);

        this.text = text;
        this.font = Client.getInstance().font;

        this.setPos(position);
        this.setMotion(MOTION);
    }

    @Override
    protected void renderParticle(MatrixStack matrices, float delta) {
        matrices.scale(-PARTICLE_SCALING);
        font.render(VertexConsumer.FONT_WORLD, matrices, 0, 0, text, TextUtils.Alignment.CENTER);
    }

    public boolean shouldRender() {
        Vector3f cam = Client.getInstance().camera.getPos();
        return cam.distanceSquared(getPos()) < 256;
    }
}
