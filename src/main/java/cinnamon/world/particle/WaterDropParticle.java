package cinnamon.world.particle;

import cinnamon.math.Rotation;
import cinnamon.registry.ParticlesRegistry;
import cinnamon.render.Camera;
import cinnamon.render.MatrixStack;
import org.joml.Math;
import org.joml.Vector3f;

public class WaterDropParticle extends SpriteParticle {

    public WaterDropParticle(int lifetime, int color) {
        super(ParticlesRegistry.WATER_DROP.texture, lifetime, color);
        this.billboard = false;
    }

    @Override
    protected void renderParticle(Camera camera, MatrixStack matrices, float delta) {
        Vector3f camPos = camera.getPos();
        float angle = Math.atan2(camPos.x - pos.x, camPos.z - pos.z) + Math.PI_f;
        matrices.rotate(Rotation.Y.rotation(angle));
        super.renderParticle(camera, matrices, delta);
    }

    @Override
    public ParticlesRegistry getType() {
        return ParticlesRegistry.WATER_DROP;
    }
}
