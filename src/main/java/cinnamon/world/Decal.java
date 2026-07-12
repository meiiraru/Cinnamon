package cinnamon.world;

import cinnamon.math.collision.AABB;
import cinnamon.model.ModelTransform;
import cinnamon.render.DebugRenderer;
import cinnamon.render.MatrixStack;
import cinnamon.utils.Resource;

public class Decal extends WorldObject {

    private final Resource albedoTexture;
    private final int lifetime;
    private int age;
    private int fadeOutTime = 20;

    public Decal(int lifetime, Resource albedoTexture) {
        super(new ModelTransform());
        this.lifetime = lifetime;
        this.albedoTexture = albedoTexture;
        this.setCastShadows(false);
    }

    @Override
    public void tick() {
        super.tick();
        age++;
    }

    public void renderDebugHitbox(MatrixStack matrices, float delta) {
        //aabb area
        DebugRenderer.renderAABB(matrices, getAABB(), 0xFFFFFFFF);

        //transform box
        matrices.pushMatrix();
        getTransform().applyTransform(matrices);
        DebugRenderer.renderAABB(matrices, new AABB(-0.5f, -0.5f, -0.5f, 0.5f, 0.5f, 0.5f), 0xFFFF72AD);
        DebugRenderer.renderArrow(matrices, 0, 0, 1, 1f, 0xFF0000FF);
        matrices.popMatrix();
    }

    @Override
    public void calculateBounds() {
        aabb.set(-0.5f, -0.5f, -0.5f, 0.5f, 0.5f, 0.5f);
        aabb.applyMatrix(getTransform().getMatrix().pos());
    }

    public Resource getAlbedoTexture() {
        return albedoTexture;
    }

    public boolean isRemoved() {
        return age >= lifetime;
    }

    public void setFadeOutTime(int fadeOutTime) {
        this.fadeOutTime = fadeOutTime;
    }

    public int getFadeOutTime() {
        return fadeOutTime;
    }

    public float getOpacity() {
        if (age >= lifetime - fadeOutTime)
            return 1f - (float) (age - (lifetime - fadeOutTime)) / fadeOutTime;
        return 1f;
    }

    @Override
    public AABB getAABB() {
        if (transform.isDirty())
            calculateBounds();
        return super.getAABB();
    }

    @Override
    public DecalType getType() {
        return DecalType.OTHER;
    }

    public enum DecalType {
        BULLET_HOLE,
        GRAFFITI,
        OTHER
    }
}
