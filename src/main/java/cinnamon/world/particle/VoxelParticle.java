package cinnamon.world.particle;

import cinnamon.model.GeometryHelper;
import cinnamon.render.Camera;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;

public class VoxelParticle extends Particle3D {

    private float size = 0.3f;
    private int color;
    private int despawnTime = 10;

    public VoxelParticle(int color, int lifetime) {
        super(null, lifetime);
        this.color = color;
    }

    @Override
    protected void renderParticle(Camera camera, MatrixStack matrices, float delta) {
        VertexConsumer consumer = emissive ? VertexConsumer.WORLD_MAIN_EMISSIVE : VertexConsumer.WORLD_MAIN;

        float s = size * 0.5f;
        applyDespawnTransform(matrices, delta);

        consumer.consume(GeometryHelper.box(matrices, -s, -s, -s, s, s, s, color));
    }

    protected void applyDespawnTransform(MatrixStack matrices, float delta) {
        float age = this.age + delta;
        if (age > lifetime - despawnTime)
            matrices.scale(1f - (age - (lifetime - despawnTime)) / despawnTime);
    }

    public void setSize(float size) {
        this.size = size;
    }

    public float getSize() {
        return size;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public int getColor() {
        return color;
    }

    public void setDespawnTime(int despawnTime) {
        this.despawnTime = despawnTime;
    }

    public int getDespawnTime() {
        return despawnTime;
    }

    @Override
    protected void updateAABB() {
        super.updateAABB();
        aabb.inflate(size * 0.5f);
    }
}
