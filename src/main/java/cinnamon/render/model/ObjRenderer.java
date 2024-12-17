package cinnamon.render.model;

import cinnamon.model.material.Material;
import cinnamon.model.obj.Face;
import cinnamon.model.obj.Group;
import cinnamon.model.obj.Mesh;
import cinnamon.registry.MaterialRegistry;
import cinnamon.render.MaterialApplier;
import cinnamon.render.MatrixStack;
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

public class ObjRenderer extends ModelRenderer {

    private final Mesh mesh;

    private final Map<String, GroupData> groups;
    private final Vector3f
            bbMin = new Vector3f(Integer.MAX_VALUE),
            bbMax = new Vector3f(Integer.MIN_VALUE);

    public ObjRenderer(ObjRenderer other) {
        this.mesh = other.mesh;
        this.groups = other.groups;
        this.bbMin.set(other.bbMin);
        this.bbMax.set(other.bbMax);
    }

    public ObjRenderer(Mesh mesh) {
        this.mesh = mesh;
        this.groups = new HashMap<>(mesh.getGroups().size(), 1f);

        //grab mesh data
        List<Vector3f> vertices = mesh.getVertices();
        List<Vector2f> uvs = mesh.getUVs();
        List<Vector3f> normals = mesh.getNormals();

        //iterate groups
        for (Group group : mesh.getGroups()) {
            //vertex list and capacity
            List<VertexData> sortedVertices = new ArrayList<>();

            //group min and max
            Vector3f groupMin = new Vector3f(Integer.MAX_VALUE);
            Vector3f groupMax = new Vector3f(Integer.MIN_VALUE);

            //iterate faces
            for (Face face : group.getFaces()) {
                //indexes
                List<Integer> v = face.getVertices();
                List<Integer> vt = face.getUVs();
                List<Integer> vn = face.getNormals();

                //vertex list
                List<VertexData> data = new ArrayList<>();

                for (int i = 0; i < v.size(); i++) {
                    //parse indexes to their actual values
                    Vector3f a = vertices.get(v.get(i));
                    Vector2f b = null;
                    Vector3f c = null;

                    //calculate min and max
                    this.bbMin.min(a);
                    this.bbMax.max(a);
                    groupMin.min(a);
                    groupMax.max(a);

                    if (!vt.isEmpty())
                        b = uvs.get(vt.get(i));
                    if (!vn.isEmpty())
                        c = normals.get(vn.get(i));

                    //add to vertex list
                    data.add(new VertexData(a, b, c));
                }

                //triangulate the faces using ear clipping
                List<VertexData> sorted = VertexData.triangulate(data);

                //calculate tangent
                VertexData.calculateTangents(sorted);

                //add data to the vertex list
                sortedVertices.addAll(sorted);
            }

            //create a new group - the group contains the OpenGL attributes
            GroupData groupData = new GroupData(group, sortedVertices, groupMin, groupMax);

            String groupName = group.getName();
            String newName = groupName;
            for (int i = 1; this.groups.containsKey(newName); i++)
                newName = groupName + "_" + i;

            this.groups.put(newName, groupData);
        }
    }

    @Override
    public void free() {
        for (GroupData group : groups.values())
            group.free();
    }

    @Override
    public void render(MatrixStack matrices) {
        render(matrices, null);
    }

    @Override
    public void render(MatrixStack matrices, Material material) {
        Shader.activeShader.applyMatrixStack(matrices);
        for (String group : groups.keySet())
            renderGroup(group, material);
    }

    @Override
    public void renderWithoutMaterial(MatrixStack matrices) {
        Shader.activeShader.applyMatrixStack(matrices);
        for (String group : groups.keySet())
            renderGroupWithoutMaterial(group);
    }

    @Override
    public AABB getAABB() {
        return new AABB(bbMin, bbMax);
    }

    @Override
    public List<AABB> getPreciseAABB() {
        List<AABB> list = new ArrayList<>();
        for (GroupData data : groups.values())
            list.add(new AABB(data.bbMin, data.bbMax));
        return list;
    }

    public Mesh getMesh() {
        return mesh;
    }

    protected void renderGroup(String name, Material material) {
        GroupData group = groups.get(name);

        //bind material
        Material mat = material == null ? group.material : material;
        int texCount;
        if (mat == null || (texCount = MaterialApplier.applyMaterial(mat)) == -1)
            texCount = MaterialApplier.applyMaterial(MaterialRegistry.MISSING);

        //render group
        group.render();

        //unbind all used textures
        Texture.unbindAll(texCount);
    }

    protected void renderGroupWithoutMaterial(String name) {
        groups.get(name).render();
    }

    private static final class GroupData {
        private final int vao, vbo, vertexCount;
        private final Material material;
        private final Vector3f bbMin, bbMax;

        public GroupData(Group group, List<VertexData> sortedVertices, Vector3f bbMin, Vector3f bbMax) {
            this.vertexCount = sortedVertices.size();
            this.material = group.getMaterial();
            this.bbMin = bbMin;
            this.bbMax = bbMax;
            Pair<Integer, Integer> buffers = generateBuffers(sortedVertices);
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
