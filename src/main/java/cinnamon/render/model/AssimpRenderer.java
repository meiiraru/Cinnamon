package cinnamon.render.model;

import cinnamon.model.Vertex;
import cinnamon.model.assimp.Mesh;
import cinnamon.model.assimp.Model;
import cinnamon.model.material.Material;
import cinnamon.registry.MaterialRegistry;
import cinnamon.render.MaterialApplier;
import cinnamon.render.MatrixStack;
import cinnamon.render.shader.Attributes;
import cinnamon.render.shader.Shader;
import cinnamon.render.texture.Texture;
import cinnamon.utils.AABB;
import cinnamon.utils.Pair;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL30.glBindVertexArray;

public class AssimpRenderer extends ModelRenderer {

    private final Model model;

    private final Map<String, MeshData> meshes;
    private final AABB aabb = new AABB();

    public AssimpRenderer(AssimpRenderer other) {
        this.model = other.model;
        this.meshes = other.meshes;
        this.aabb.set(other.aabb);
    }

    public AssimpRenderer(Model model) {
        this.model = model;
        this.meshes = new HashMap<>(model.meshes.size(), 1f);

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
            MeshData data = new MeshData(mesh, vertices, material);

            String name = mesh.name;
            String newName = name;
            for (int i = 1; this.meshes.containsKey(newName); i++)
                newName = name + "_" + i;

            this.meshes.put(newName, data);
        }
    }

    @Override
    public void free() {
        for (MeshData mesh : meshes.values())
            mesh.free();
    }

    @Override
    public void render(MatrixStack matrices) {
        render(matrices, null);
    }

    @Override
    public void render(MatrixStack matrices, Material material) {
        Shader.activeShader.applyMatrixStack(matrices);
        for (String mesh : meshes.keySet())
            renderMesh(mesh, material);
    }

    @Override
    public void renderWithoutMaterial(MatrixStack matrices) {
        Shader.activeShader.applyMatrixStack(matrices);
        for (String mesh : meshes.keySet())
            renderMeshWithoutMaterial(mesh);
    }

    @Override
    public AABB getAABB() {
        return new AABB(aabb);
    }

    @Override
    public List<AABB> getPreciseAABB() {
        List<AABB> list = new ArrayList<>();
        for (MeshData mesh : meshes.values())
            list.add(new AABB(mesh.aabb));
        return list;
    }

    public Model getModel() {
        return model;
    }

    protected void renderMesh(String name, Material material) {
        MeshData mesh = meshes.get(name);

        //bind material
        Material mat = material == null ? mesh.material : material;
        int texCount;
        if (mat == null || (texCount = MaterialApplier.applyMaterial(mat)) == -1)
            texCount = MaterialApplier.applyMaterial(MaterialRegistry.MISSING);

        //render mesh
        mesh.render();

        //unbind all used textures
        Texture.unbindAll(texCount);
    }

    protected void renderMeshWithoutMaterial(String name) {
        meshes.get(name).render();
    }

    private static final class MeshData {
        private final int vao, vbo, vertexCount;
        private final Material material;
        private final AABB aabb;

        public MeshData(Mesh mesh, List<Vertex> vertices, Material material) {
            this.vertexCount = vertices.size();
            this.material = material;
            this.aabb = new AABB(mesh.aabb);
            Pair<Integer, Integer> buffers = generateBuffers(vertices, Attributes.POS, Attributes.UV_FLIP, Attributes.NORMAL, Attributes.TANGENTS);
            this.vao = buffers.first();
            this.vbo = buffers.second();
        }

        public void render() {
            //bind vao
            glBindVertexArray(vao);

            //draw
            glDrawArrays(GL_TRIANGLES, 0, vertexCount);
        }

        public void free() {
            glDeleteBuffers(vao);
            glDeleteBuffers(vbo);
        }
    }
}
