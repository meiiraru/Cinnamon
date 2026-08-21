package cinnamon.world.terrain;

import cinnamon.model.material.Material;
import cinnamon.registry.TerrainModelRegistry;
import cinnamon.registry.TerrainRegistry;
import cinnamon.render.Camera;
import cinnamon.render.MatrixStack;

public class Glass extends Terrain {

    public Glass() {
        super(TerrainModelRegistry.GLASS.resource, TerrainRegistry.GLASS);
    }

    @Override
    public void render(Camera camera, MatrixStack matrices, float delta) {
        super.render(camera, matrices, delta);
    }

    @Override
    public void renderTransparent(Camera camera, MatrixStack matrices, float delta) {
        if (model == null)
            return;

        matrices.pushMatrix();
        applyModelPose(matrices, delta);

        Material mat = model.getMaterials().get("glass");
        model.render(matrices, mat);

        matrices.popMatrix();

        super.renderTransparent(camera, matrices, delta);
    }

    @Override
    public boolean isTransparent() {
        return true;
    }
}
