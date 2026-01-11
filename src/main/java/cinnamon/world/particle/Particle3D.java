package cinnamon.world.particle;

import cinnamon.model.ModelManager;
import cinnamon.registry.ParticlesRegistry;
import cinnamon.render.Camera;
import cinnamon.render.MatrixStack;
import cinnamon.render.model.ModelRenderer;
import cinnamon.utils.Resource;

public class Particle3D extends Particle {

    private final ModelRenderer model;

    public Particle3D(Resource model, int lifetime) {
        super(lifetime);
        this.billboard = false;
        this.model = ModelManager.load(model);
        updateAABB();
    }

    @Override
    protected void renderParticle(Camera camera, MatrixStack matrices, float delta) {
        if (model != null)
            model.render(matrices);
    }

    @Override
    protected void updateAABB() {
        if (model == null) {
            super.updateAABB();
            return;
        }

        aabb.set(model.getAABB());
        aabb.translate(getPos());
    }

    @Override
    public ParticlesRegistry getType() {
        return ParticlesRegistry.OTHER;
    }
}
