package cinnamon.world.particle;

import cinnamon.model.GeometryHelper;
import cinnamon.model.Vertex;
import cinnamon.registry.ParticlesRegistry;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.render.texture.SpriteTexture;
import cinnamon.utils.Maths;

public abstract class SpriteParticle extends Particle {

    protected final SpriteTexture texture;
    private int color;
    private float scale = 1f;

    public SpriteParticle(int lifetime, int color) {
        super(lifetime);
        ParticlesRegistry type = getType();
        this.texture = type.getTexture();
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
                texture.getUFrames(), texture.getVFrames()
        );

        drawParticle(delta, isEmissive() ? VertexConsumer.MAIN : VertexConsumer.WORLD_MAIN, vertices);
    }

    protected void drawParticle(float delta, VertexConsumer consumer, Vertex[] vertices) {
        int color = getColor();
        for (Vertex vertex : vertices)
            vertex.color(color);

        consumer.consume(vertices, texture.getResource());
    }

    public int getCurrentFrame() {
        return Math.round(Maths.lerp(0, texture.getUFrames() - 1, (float) getAge() / getLifetime()));
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
