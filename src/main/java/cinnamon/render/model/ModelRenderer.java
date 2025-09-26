package cinnamon.render.model;

import cinnamon.model.material.Material;
import cinnamon.registry.MaterialRegistry;
import cinnamon.render.MaterialApplier;
import cinnamon.render.MatrixStack;
import cinnamon.render.shader.Shader;
import cinnamon.render.texture.Texture;
import cinnamon.utils.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class ModelRenderer {

    protected final Map<String, MeshData> meshes;
    protected final AABB aabb = new AABB();

    public ModelRenderer(Map<String, MeshData> meshes) {
        this.meshes = meshes;
    }

    public void free() {
        for (MeshData mesh : meshes.values())
            mesh.free();
    }

    public void render(MatrixStack matrices) {
        render(matrices, null);
    }

    public void render(MatrixStack matrices, Material material) {
        Shader.activeShader.applyMatrixStack(matrices);
        for (MeshData mesh : meshes.values())
            renderMesh(mesh, material);
    }

    public void renderWithoutMaterial(MatrixStack matrices) {
        Shader.activeShader.applyMatrixStack(matrices);
        for (MeshData mesh : meshes.values())
            renderMeshWithoutMaterial(mesh);
    }

    protected void renderMesh(MeshData mesh, Material material) {
        //bind material
        Material mat = material == null ? mesh.getMaterial() : material;
        int texCount;
        if (mat == null || (texCount = MaterialApplier.applyMaterial(mat, 0)) == -1)
            texCount = MaterialApplier.applyMaterial(MaterialRegistry.MISSING, 0);

        //render mesh
        mesh.render();

        //unbind all used textures
        Texture.unbindAll(texCount);
    }

    protected void renderMeshWithoutMaterial(MeshData mesh) {
        mesh.render();
    }

    public AABB getAABB() {
        return new AABB(aabb);
    }

    public List<AABB> getPreciseAABB() {
        List<AABB> list = new ArrayList<>();
        for (MeshData mesh : meshes.values())
            list.add(new AABB(mesh.getAABB()));
        return list;
    }
}
