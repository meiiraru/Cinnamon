package cinnamon.world.particle;

import cinnamon.math.Rotation;
import cinnamon.registry.ParticlesRegistry;
import cinnamon.render.Camera;
import cinnamon.render.MatrixStack;

public class RippleParticle extends SpriteParticle {

    public RippleParticle(int lifetime, int color) {
        super(ParticlesRegistry.RIPPLE.texture, lifetime, color);
        billboard = false;
    }

    @Override
    protected void renderParticle(Camera camera, MatrixStack matrices, float delta) {
        matrices.pushMatrix();
        matrices.rotate(Rotation.X.rotationDeg(90));
        super.renderParticle(camera, matrices, delta);
        matrices.popMatrix();
    }

    @Override
    public ParticlesRegistry getType() {
        return ParticlesRegistry.RIPPLE;
    }
}
