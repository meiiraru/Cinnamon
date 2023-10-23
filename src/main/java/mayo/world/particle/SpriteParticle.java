package mayo.world.particle;

import mayo.model.GeometryHelper;
import mayo.model.Vertex;
import mayo.render.MatrixStack;
import mayo.render.Texture;
import mayo.render.batch.VertexConsumer;
import mayo.utils.Maths;
import mayo.world.World;

public abstract class SpriteParticle extends Particle {

    private final Texture texture;
    private int color;
    private float scale = 1f;

    public SpriteParticle(Texture texture, World world, int lifetime, int color) {
        super(world, lifetime);
        this.texture = texture;
        this.color = color;
    }

    @Override
    protected void renderParticle(MatrixStack matrices, float delta) {
        matrices.push();
        float s = PARTICLE_SCALING * scale;
        matrices.scale(-s, -s, s);

        Vertex[] vertices = GeometryHelper.quad(
                matrices,
                -8, -8,
                16, 16,
                getCurrentFrame(), 0f,
                1, 1,
                texture.getuFrames(), texture.getvFrames()
        );

        for (Vertex vertex : vertices)
            vertex.color(getColor());

        VertexConsumer.MAIN.consume(vertices, texture.getID());

        matrices.pop();
    }

    public int getCurrentFrame() {
        return Math.round(Maths.lerp(0, this.texture.getuFrames() - 1, (float) getAge() / getLifetime()));
    }

    public void setColor(int color) {
        this.color = color;
    }

    public int getColor() {
        return color;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    public float getScale() {
        return scale;
    }
}
