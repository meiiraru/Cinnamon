package cinnamon.world.terrain;

import cinnamon.math.Maths;
import cinnamon.model.material.Material;
import cinnamon.registry.TerrainModelRegistry;
import cinnamon.registry.TerrainRegistry;
import cinnamon.render.Camera;
import cinnamon.render.MatrixStack;

public class Rose extends Terrain {

    private Material variant;

    public Rose() {
        super(TerrainModelRegistry.ROSE.resource, TerrainRegistry.ROSE);
        getCollisionMask().setExcludeMask(0, true);
        setVariant(Maths.randomArr(Variant.values()));
    }

    @Override
    protected void renderModel(Camera camera, Material material, MatrixStack matrices, float delta) {
        if (material == null && model != null)
            material = variant;
        super.renderModel(camera, material, matrices, delta);
    }

    public void setVariant(Variant variant) {
        if (model == null)
            return;

        this.variant = variant == null ? null : model.getMaterials().get(variant.name().toLowerCase());
    }

    public Material getVariant() {
        return variant;
    }

    public enum Variant {
        RED, WHITE, PINK, BLACK
    }
}
