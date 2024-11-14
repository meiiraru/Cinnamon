package cinnamon.render.model;

import cinnamon.model.material.Material;
import cinnamon.model.obj.Face;
import cinnamon.model.obj.Group;
import cinnamon.model.obj.Mesh;
import cinnamon.registry.MaterialRegistry;
import cinnamon.render.MaterialApplier;
import cinnamon.render.MatrixStack;
import cinnamon.render.shader.Attributes;
import cinnamon.render.shader.Shader;
import cinnamon.render.texture.Texture;
import cinnamon.utils.AABB;
import cinnamon.utils.Maths;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

public class ObjRenderer extends ModelRenderer {

    private final Mesh mesh;

    private final Map<String, GroupData> groups;
    private final Vector3f
            bbMin = new Vector3f(Integer.MAX_VALUE),
            bbMax = new Vector3f(Integer.MIN_VALUE);

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
            int capacity = 0;

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

                //increase needed capacity
                //pos, uv?, norm?, tangent
                capacity += sorted.size() * (3 + (vt.isEmpty() ? 0 : 2) + (vn.isEmpty() ? 0 : 3) + 3);

                //add data to the vertex list
                sortedVertices.addAll(sorted);
            }

            //create a new group - the group contains the OpenGL attributes
            GroupData groupData = new GroupData(group, sortedVertices.size(), capacity, groupMin, groupMax);
            this.groups.put(group.getName(), groupData);

            //different buffer per group
            FloatBuffer buffer = BufferUtils.createFloatBuffer(capacity);

            //push vertices to buffer
            for (VertexData data : sortedVertices)
                data.pushToBuffer(buffer);

            //bind buffer to the current VBO
            buffer.rewind();
            glBufferSubData(GL_ARRAY_BUFFER, 0, buffer);
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
        group.render(material == null ? group.material : material);
    }

    protected void renderGroupWithoutMaterial(String name) {
        groups.get(name).renderWithoutMaterial();
    }

    private static final class VertexData {
        private static final Vector3f DEFAULT_TANGENT = new Vector3f(0, 0, -1);

        private final Vector3f pos, norm;
        private final Vector2f uv;
        private Vector3f tangent;

        private VertexData(Vector3f pos, Vector2f uv, Vector3f norm) {
            this.pos = pos;
            this.uv = uv;
            this.norm = norm;
            this.tangent = DEFAULT_TANGENT;
        }

        private void pushToBuffer(FloatBuffer buffer) {
            //push pos
            buffer.put(pos.x);
            buffer.put(pos.y);
            buffer.put(pos.z);

            //push uv
            if (uv != null) {
                buffer.put(uv.x);
                buffer.put(1 - uv.y); //invert Y
            }

            //push normal
            if (norm != null) {
                buffer.put(norm.x);
                buffer.put(norm.y);
                buffer.put(norm.z);
            }

            //push tangent
            buffer.put(tangent.x);
            buffer.put(tangent.y);
            buffer.put(tangent.z);
        }

        private static List<VertexData> triangulate(List<VertexData> data) {
            //list to return
            List<VertexData> triangles = new ArrayList<>();

            while (data.size() >= 3) {
                int n = data.size();
                boolean earFound = false;

                //iterate through all the vertices to find an ear
                for (int i = 0; i < n; i++) {
                    int prev = (i - 1 + n) % n;
                    int next = (i + 1) % n;

                    //triangle
                    VertexData a = data.get(prev);
                    VertexData b = data.get(i);
                    VertexData c = data.get(next);

                    //check if the current triangle is an ear
                    if (isEar(a.pos, b.pos, c.pos, data)) {
                        //if so, add the ear to the return list
                        triangles.add(a);
                        triangles.add(b);
                        triangles.add(c);

                        //and remove our anchor vertex from the list
                        data.remove(i);
                        earFound = true;
                        break;
                    }
                }

                //if the polygon is self-intersecting or has holes, stop the triangulation
                if (!earFound)
                    break;
            }

            //return the new list of triangles
            return triangles;
        }

        private static boolean isEar(Vector3f a, Vector3f b, Vector3f c, List<VertexData> list) {
            for (VertexData d : list) {
                Vector3f point = d.pos;

                //continue if the point is one of the triangle vertices
                if (point.equals(a) || point.equals(b) || point.equals(c))
                    continue;

                //check if out point is inside the triangle
                if (Maths.isPointInTriangle(a, b, c, point))
                    return false;
            }

            return true;
        }

        private static void calculateTangents(List<VertexData> list) {
            for (int i = 0; i < list.size(); i += 3) {
                VertexData v0 = list.get(i);
                VertexData v1 = list.get(i + 1);
                VertexData v2 = list.get(i + 2);

                //vertex uv may be null
                if (v0.uv == null || v1.uv == null || v2.uv == null)
                    continue;

                //calculate tangent vector
                Vector3f edge1 = new Vector3f(v1.pos).sub(v0.pos);
                Vector3f edge2 = new Vector3f(v2.pos).sub(v0.pos);

                float deltaU1 = v1.uv.x - v0.uv.x;
                float deltaV1 = v1.uv.y - v0.uv.y;
                float deltaU2 = v2.uv.x - v0.uv.x;
                float deltaV2 = v2.uv.y - v0.uv.y;

                float f = 1f / (deltaU1 * deltaV2 - deltaU2 * deltaV1);
                Vector3f tangent = new Vector3f(
                        f * (deltaV2 * edge1.x - deltaV1 * edge2.x),
                        f * (deltaV2 * edge1.y - deltaV1 * edge2.y),
                        f * (deltaV2 * edge1.z - deltaV1 * edge2.z)
                ).normalize();

                //set tangent to the vertices
                v0.tangent = tangent;
                v1.tangent = tangent;
                v2.tangent = tangent;
            }
        }
    }

    private static final class GroupData {
        private final int vao, vbo, vertexCount;
        private final Material material;
        private final Vector3f bbMin, bbMax;

        private GroupData(Group group, int vertexCount, int capacity, Vector3f bbMin, Vector3f bbMax) {
            this.vertexCount = vertexCount;
            this.material = group.getMaterial();
            this.bbMin = bbMin;
            this.bbMax = bbMax;

            //parse attributes
            Face face = group.getFaces().getFirst();
            List<Attributes> list = new ArrayList<>();
            list.add(Attributes.POS);

            if (!face.getUVs().isEmpty())
                list.add(Attributes.UV);
            if (!face.getNormals().isEmpty())
                list.add(Attributes.NORMAL);

            list.add(Attributes.TANGENTS);

            Attributes[] flags = list.toArray(new Attributes[0]);
            int vertexSize = Attributes.getVertexSize(flags);

            //vao
            this.vao = glGenVertexArrays();
            glBindVertexArray(vao);

            //vbo
            this.vbo = glGenBuffers();
            glBindBuffer(GL_ARRAY_BUFFER, vbo);
            glBufferData(GL_ARRAY_BUFFER, (long) capacity * Float.BYTES, GL_STATIC_DRAW);

            //load vertex attributes
            Attributes.load(flags, vertexSize);

            //enable attributes
            for (int i = 0; i < flags.length; i++)
                glEnableVertexAttribArray(i);
        }

        private void renderWithoutMaterial() {
            //bind vao
            glBindVertexArray(vao);

            //draw
            glDrawArrays(GL_TRIANGLES, 0, vertexCount);
        }

        private void render(Material material) {
            //bind material
            int texCount;
            if (material == null || (texCount = MaterialApplier.applyMaterial(material)) == -1)
                texCount = MaterialApplier.applyMaterial(MaterialRegistry.MISSING);

            //render model
            renderWithoutMaterial();

            //unbind all used textures
            Texture.unbindAll(texCount);
        }

        private void free() {
            glDeleteBuffers(vao);
            glDeleteBuffers(vbo);
        }
    }
}
