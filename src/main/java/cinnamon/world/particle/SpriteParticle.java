package cinnamon.world.particle;

import cinnamon.model.GeometryHelper;
import cinnamon.model.Vertex;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.render.texture.Texture;
import cinnamon.utils.Maths;
import cinnamon.utils.Resource;

public abstract class SpriteParticle extends Particle {

    protected final Resource texture;
    protected int color;
    protected float scale = 1f;

    public SpriteParticle(Resource texture, int lifetime, int color) {
        super(lifetime);
        this.texture = texture;
        this.color = color;
    }

    @Override
    protected void renderParticle(MatrixStack matrices, float delta) {
        float s = PARTICLE_SCALING * scale;
        matrices.scale(-s, -s, s);

        Vertex[] vertices = GeometryHelper.quad(
                matrices,
                -8, -8,
                16, 16,
                getCurrentFrame(), 0f,
                1, 1,
                getFrameCount(), 1
        );

        drawParticle(delta, isEmissive() ? VertexConsumer.MAIN : VertexConsumer.WORLD_MAIN, vertices);
    }

    protected void drawParticle(float delta, VertexConsumer consumer, Vertex[] vertices) {
        int color = getColor();
        for (Vertex vertex : vertices)
            vertex.color(color);

        consumer.consume(vertices, texture);
    }

    public int getCurrentFrame() {
        return Math.round(Maths.lerp(0, getFrameCount() - 1, (float) getAge() / getLifetime()));
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

    public int getFrameCount() {
        Texture tex = Texture.of(texture);
        return Math.max(tex.getWidth() / tex.getHeight(), 1);
    }

    @Override
    protected void updateAABB() {
        super.updateAABB();
        aabb.inflate(8 * PARTICLE_SCALING * scale);
    }
}
