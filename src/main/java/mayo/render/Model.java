package mayo.render;

import mayo.model.obj.Mesh;
import mayo.model.obj.material.Material;
import mayo.utils.AABB;

import java.util.List;

public abstract class Model {

    private final Mesh mesh;

    public Model(Mesh mesh) {
        this.mesh = mesh;
    }

    public abstract void render();

    public abstract void renderWithoutMaterial();

    public abstract AABB getMeshAABB();

    public abstract List<AABB> getGroupsAABB();

    public Mesh getMesh() {
        return mesh;
    }

    public abstract void free();

    public abstract void setOverrideMaterial(Material material);
}
