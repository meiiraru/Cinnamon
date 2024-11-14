package cinnamon.render.model;

import cinnamon.model.material.Material;
import cinnamon.render.MatrixStack;
import cinnamon.utils.AABB;

import java.util.List;

public abstract class ModelRenderer {

    public abstract void free();

    public abstract void render(MatrixStack matrices);

    public abstract void render(MatrixStack matrices, Material material);

    public abstract void renderWithoutMaterial(MatrixStack matrices);

    public abstract AABB getAABB();

    public abstract List<AABB> getPreciseAABB();
}
