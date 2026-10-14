package cinnamon.world.particle;

import cinnamon.registry.ParticlesRegistry;
import cinnamon.render.Camera;
import cinnamon.render.MatrixStack;
import org.joml.Math;

public class LeafParticle extends SpriteParticle {

    private int rotSpeed = 5;
    protected int oRot, rot = 0;

    public LeafParticle(int lifetime, int color) {
        super(ParticlesRegistry.LEAF.texture, lifetime, color);
        this.setMotion(0.1f, -0.1f, 0.1f);
    }

    @Override
    public void tick() {
        super.tick();
        if (collideTerrain())
            getMotion().zero();

        oRot = rot;
        if (getMotion().lengthSquared() > 0.0001f)
            rot += rotSpeed;
    }

    @Override
    protected void renderParticle(Camera camera, MatrixStack matrices, float delta) {
        float age = getAge() + delta;
        int lifetime = getLifetime();
        int delay = 10;
        if (age >= lifetime - delay) {
            float d = (age - (lifetime - delay)) / delay;
            setScale(Math.lerp(getScale(), 0, d));
        }

        matrices.rotateZ(Math.lerp(oRot, rot, delta));

        super.renderParticle(camera, matrices, delta);
    }

    @Override
    public ParticlesRegistry getType() {
        return ParticlesRegistry.LEAF;
    }

    public void setRotSpeed(int rotSpeed) {
        this.rotSpeed = rotSpeed;
    }

    public int getRotSpeed() {
        return rotSpeed;
    }
}
