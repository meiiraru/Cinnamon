package mayo.render;

import mayo.model.obj.Face;
import mayo.model.obj.Group;
import mayo.model.obj.Mesh;
import mayo.model.obj.material.Material;
import mayo.model.obj.material.MtlMaterial;
import mayo.model.obj.material.PBRMaterial;
import mayo.render.shader.Attributes;
import mayo.render.shader.Shader;
import mayo.utils.AABB;
import mayo.utils.Maths;
import mayo.utils.Pair;
import mayo.utils.Resource;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

public class OpenGLModel extends Model {

    private final List<GroupData> groups = new ArrayList<>();
    private final Vector3f
            bbMin = new Vector3f(Integer.MAX_VALUE),
            bbMax = new Vector3f(Integer.MIN_VALUE);

    public OpenGLModel(Mesh mesh) {
        super(mesh);

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
                VertexData.calculateTangent(sorted);

                //increase needed capacity
                capacity += sorted.size() * (3 + (vt.isEmpty() ? 0 : 2) + (vn.isEmpty() ? 0 : 3) + 3);

                //add data to the vertex list
                sortedVertices.addAll(sorted);
            }

            //create a new group - the group contains the OpenGL attributes
            GroupData groupData = new GroupData(group, sortedVertices.size(), capacity, groupMin, groupMax);
            this.groups.add(groupData);

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
    public void render() {
        for (GroupData group : groups)
            group.render();
    }

    @Override
    public AABB getMeshAABB() {
        return new AABB(bbMin, bbMax);
    }

    @Override
    public List<AABB> getGroupsAABB() {
        List<AABB> list = new ArrayList<>();
        for (GroupData data : groups)
            list.add(new AABB(data.bbMin, data.bbMax));
        return list;
    }

    @Override
    public void free() {
        for (GroupData group : groups)
            group.free();
    }

    @Override
    public void setOverrideMaterial(Material material) {
        MaterialData mat = material == null ? null : new MaterialData(material);
        for (GroupData group : groups)
            group.overrideMaterial = mat;
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

        private static void calculateTangent(List<VertexData> list) {
            for (int i = 0; i < list.size(); i += 3) {
                VertexData v0 = list.get(i);
                VertexData v1 = list.get(i + 1);
                VertexData v2 = list.get(i + 2);

                //vertex uv may be null
                if (v0.uv == null || v1.uv == null || v2.uv == null)
                    continue;

                Vector3f edge1 = new Vector3f(v1.pos).sub(v0.pos);
                Vector3f edge2 = new Vector3f(v2.pos).sub(v0.pos);

                Vector2f deltaUV1 = new Vector2f(v1.uv).sub(v0.uv);
                Vector2f deltaUV2 = new Vector2f(v2.uv).sub(v0.uv);

                float r = 1f / (deltaUV1.x * deltaUV2.y - deltaUV2.x * deltaUV1.y);

                //tangent = (edge1 * deltaUV2.y - edge2 * deltaUV1.y) * r;
                Vector3f tangent = new Vector3f(
                        (edge1.x * deltaUV2.y - edge2.x * deltaUV1.y) * r,
                        (edge1.y * deltaUV2.y - edge2.y * deltaUV1.y) * r,
                        (edge1.z * deltaUV2.y - edge2.z * deltaUV1.y) * r
                );

                v0.tangent = tangent;
                v1.tangent = tangent;
                v2.tangent = tangent;
            }
        }
    }

    private static class GroupData {
        private final int vao, vbo, vertexCount;
        private final MaterialData material;
        private final Vector3f bbMin, bbMax;
        private MaterialData overrideMaterial;

        private GroupData(Group group, int vertexCount, int capacity, Vector3f bbMin, Vector3f bbMax) {
            this.vertexCount = vertexCount;
            this.material = new MaterialData(group.getMaterial());
            this.bbMin = bbMin;
            this.bbMax = bbMax;

            //parse attributes
            int flags = group.getFaces().getFirst().getAttributesFlag() | Attributes.TANGENTS;
            Pair<Integer, Integer> attrib = Attributes.getAttributes(flags);

            //vao
            this.vao = glGenVertexArrays();
            glBindVertexArray(vao);

            //vbo
            this.vbo = glGenBuffers();
            glBindBuffer(GL_ARRAY_BUFFER, vbo);
            glBufferData(GL_ARRAY_BUFFER, (long) capacity * Float.BYTES, GL_STATIC_DRAW);

            //load vertex attributes
            Attributes.load(flags, attrib.second());

            //enable attributes
            for (int i = 0; i < attrib.first(); i++)
                glEnableVertexAttribArray(i);
        }

        private void render() {
            //bind vao
            glBindVertexArray(vao);

            //bind material
            int texCount;
            MaterialData material = overrideMaterial != null ? overrideMaterial : this.material;
            if (material == null || (texCount = material.use()) == -1) {
                Texture.MISSING.bind();
                texCount = 1;
            }

            //draw
            glDrawArrays(GL_TRIANGLES, 0, vertexCount);

            //unbind all used textures
            for (int i = texCount - 1; i >= 0; i--)
                Texture.unbindTex(i);
        }

        private void free() {
            glDeleteBuffers(vao);
            glDeleteBuffers(vbo);
        }
    }

    private record MaterialData(Material material) {
        public int use() {
            if (material == null)
                return -1;

            Shader s = Shader.activeShader;

            if (material instanceof MtlMaterial phong) {
                s.setVec3("material.ambient", phong.getAmbientColor());
                s.setVec3("material.diffuse", phong.getDiffuseColor());
                s.setVec3("material.specular", phong.getSpecularColor());
                s.setFloat("material.shininess", phong.getSpecularExponent());

                bindTex(s, phong.getDiffuseTex(), 0, "material.diffuseTex");
                bindTex(s, phong.getSpColorTex(), 1, "material.specularTex");
                bindTex(s, phong.getEmissiveTex(), 2, "material.emissiveTex");

                return 3;
            } else if (material instanceof PBRMaterial pbr) {
                bindTex(s, pbr.getAlbedo(), 0, "material.albedoTex");
                bindTex(s, pbr.getHeight(), 1, "material.heightTex");
                bindTex(s, pbr.getNormal(), 2, "material.normalTex");
                bindTex(s, pbr.getRoughness(), 3, "material.roughnessTex");
                bindTex(s, pbr.getMetallic(), 4, "material.metallicTex");
                bindTex(s, pbr.getAO(), 5, "material.aoTex");
                bindTex(s, pbr.getEmissive(), 6, "material.emissiveTex");

                return 7;
            } else {
                return -1;
            }
        }

        private static void bindTex(Shader s, Resource res, int index, String name) {
            s.setInt(name, index);

            if (res == null) {
                Texture.unbindTex(index);
                return;
            }

            Texture tex = Texture.of(res);
            if (tex == null) {
                Texture.unbindTex(index);
                return;
            }

            glActiveTexture(GL_TEXTURE0 + index);
            tex.bind();
        }
    }
}
