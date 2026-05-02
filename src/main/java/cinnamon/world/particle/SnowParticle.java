package cinnamon.world.particle;

import cinnamon.math.Rotation;
import cinnamon.registry.ParticlesRegistry;
import cinnamon.render.Camera;
import cinnamon.render.MatrixStack;
import cinnamon.world.world.WorldClient;
import org.joml.Math;
import org.joml.Vector3f;

public class SnowParticle extends SpriteParticle {

    private final int strength;

    private boolean collided;

    public SnowParticle(int lifetime, int color, int strength) {
        super(ParticlesRegistry.SNOW.texture, lifetime, color);
        this.strength = strength;
        this.billboard = false;
        setMotion(0, -0.03f, 0);
    }

    @Override
    public void tick() {
        super.tick();

        if (!collided) {
            collided = collideTerrain() || collideEntities();
            if (collided) {
                getMotion().zero();
                age = lifetime - (getFrameCount() - 1);
            }
        }
    }

    @Override
    protected void renderParticle(Camera camera, MatrixStack matrices, float delta) {
        Vector3f camPos = camera.getPos();
        float angle = Math.atan2(camPos.x - pos.x, camPos.z - pos.z) + Math.PI_f;
        matrices.rotate(Rotation.Y.rotation(angle));
        super.renderParticle(camera, matrices, delta);
    }

    @Override
    public int getCurrentFrame() {
        return strength % getFrameCount();
    }

    @Override
    public ParticlesRegistry getType() {
        return ParticlesRegistry.SNOW;
    }
}
