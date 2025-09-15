package cinnamon.render.model;

import cinnamon.model.Vertex;
import cinnamon.model.assimp.Mesh;
import cinnamon.model.assimp.Model;
import cinnamon.model.material.Material;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class AssimpRenderer extends ModelRenderer {

    private final Model model;

    public AssimpRenderer(AssimpRenderer other) {
        super(other.meshes);
        this.aabb.set(other.aabb);
        this.model = other.model;
    }

    public AssimpRenderer(Model model) {
        super(new HashMap<>(model.meshes.size(), 1f));
        this.model = model;

        if (!model.meshes.isEmpty())
            this.aabb.set(model.meshes.getFirst().aabb);

        //iterate over meshes
        for (Mesh mesh : model.meshes) {
            //vertex list
            List<Vertex> vertices = new ArrayList<>();

            this.aabb.merge(mesh.aabb);

            //indexes
            List<Integer> indices = mesh.indices;
            for (int i : indices) {
                //parse indexes to their actual values
                Vector3f v = mesh.vertices.get(i);
                Vector2f u = mesh.hasUVs ? mesh.uvs.get(i) : Vertex.DEFAULT_UV;
                Vector3f n = mesh.hasNormals ? mesh.normals.get(i) : Vertex.DEFAULT_NORMAL;
                Vector3f t = mesh.hasTangents ? mesh.tangents.get(i) : Vertex.DEFAULT_TANGENT;

                //add to vertex list
                vertices.add(Vertex.of(v).uv(u).normal(n).tangent(t));
            }

            //create a new mesh data with the OpenGL attributes
            Material material = model.materials.get(mesh.materialIndex);
            MeshData data = new MeshData(mesh.aabb, vertices, material);

            String name = mesh.name;
            String newName = name;
            for (int i = 1; this.meshes.containsKey(newName); i++)
                newName = name + "_" + i;

            this.meshes.put(newName, data);
        }
    }

    public Model getModel() {
        return model;
    }
}
