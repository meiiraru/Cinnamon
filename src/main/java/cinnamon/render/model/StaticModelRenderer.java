package cinnamon.render.model;

import cinnamon.model.Vertex;
import cinnamon.model.material.Material;
import cinnamon.model.obj.Mesh;
import cinnamon.render.MatrixStack;
import cinnamon.utils.AABB;

import java.util.Collection;
import java.util.List;

public class StaticModelRenderer extends ObjRenderer {

    public StaticModelRenderer(Mesh mesh) {
        this(mesh, null);
    }

    public StaticModelRenderer(Mesh mesh, List<MatrixStack.Pose> transforms) {
        super(mesh);
        if (transforms != null && !transforms.isEmpty())
            updateTransforms(transforms);
    }

    public void updateTransforms(List<MatrixStack.Pose> transforms) {
        for (MeshData mesh : meshes.values())
            ((InstancedMeshData) mesh).updateInstanceBuffer(transforms);
    }

    @Override
    protected MeshData generateMesh(AABB aabb, Collection<Vertex> vertices, int[] indices, Material material) {
        return new InstancedMeshData(aabb, vertices, indices, material);
    }
}
