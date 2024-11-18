package cinnamon.render.model;

import cinnamon.model.gltf.GLTFModel;
import cinnamon.model.material.Material;
import cinnamon.render.MatrixStack;
import cinnamon.utils.AABB;

import java.util.List;

public class GLTFRenderer extends ModelRenderer {

    private final GLTFModel model;

    public GLTFRenderer(GLTFModel model) {
        this.model = model;
    }

    @Override
    public void free() {

    }

    @Override
    public void render(MatrixStack matrices) {

    }

    @Override
    public void render(MatrixStack matrices, Material material) {

    }

    @Override
    public void renderWithoutMaterial(MatrixStack matrices) {

    }

    @Override
    public AABB getAABB() {
        return new AABB();
    }

    @Override
    public List<AABB> getPreciseAABB() {
        return List.of();
    }
}
