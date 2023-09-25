package mayo.render;

import mayo.model.obj.Face;
import mayo.model.obj.Group;
import mayo.model.obj.Material;
import mayo.model.obj.Mesh;
import mayo.render.shader.Attributes;
import mayo.utils.Pair;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

public class OpenGLModel extends Model {

    private final List<GroupData> groups = new ArrayList<>();

    public OpenGLModel(Mesh mesh) {
        super(mesh);

        List<Vector3f> vertices = mesh.getVertices();
        List<Vector2f> uvs = mesh.getUVs();
        List<Vector3f> normals = mesh.getNormals();

        for (Group group : mesh.getGroups()) {
            GroupData groupData = new GroupData(group);
            this.groups.add(groupData);

            FloatBuffer buffer = BufferUtils.createFloatBuffer(groupData.getCapacity());

            for (Face face : group.getFaces()) {
                List<Integer> v = face.getVertices();
                List<Integer> vt = face.getUVs();
                List<Integer> vn = face.getNormals();

                for (int i = 0; i < v.size(); i++) {
                    fillBuffer(buffer, vertices.get(v.get(i)));
                    if (!vt.isEmpty())
                        fillBuffer(buffer, uvs.get(vt.get(i)));
                    if (!vn.isEmpty())
                        fillBuffer(buffer, normals.get(vn.get(i)));
                }
            }

            buffer.rewind();
            glBufferSubData(GL_ARRAY_BUFFER, 0, buffer);
        }
    }

    private static void fillBuffer(FloatBuffer buffer, Vector2f vec) {
        buffer.put(vec.x);
        buffer.put(vec.y);
    }

    private static void fillBuffer(FloatBuffer buffer, Vector3f vec) {
        buffer.put(vec.x);
        buffer.put(vec.y);
        buffer.put(vec.z);
    }

    @Override
    public void render() {
        for (GroupData group : groups)
            group.render();
    }

    private static class GroupData {
        private final int vao, capacity, vertexCount;
        private final MaterialData material;

        private GroupData(Group group) {
            //vertices data
            int capacity = 0;
            int vertexCount = 0;

            for (Face face : group.getFaces()) {
                capacity += face.getLength();
                vertexCount += face.getVertexCount();
            }

            this.capacity = capacity;
            this.vertexCount = vertexCount;
            this.material = new MaterialData(group.getMaterial());

            int flags = group.getFaces().get(0).getAttributesFlag();
            Pair<Integer, Integer> attrib = Attributes.getAttributes(flags);

            //vao
            this.vao = glGenVertexArrays();
            glBindVertexArray(vao);

            //vbo
            int vbo = glGenBuffers();
            glBindBuffer(GL_ARRAY_BUFFER, vbo);
            glBufferData(GL_ARRAY_BUFFER, (long) this.capacity * Float.BYTES, GL_STATIC_DRAW);

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
            if (material != null) {
                material.use();
            } else {
                Texture.MISSING.bind();
            }

            //draw
            glDrawArrays(GL_TRIANGLES, 0, vertexCount);

            //unbind texture
            glBindTexture(GL_TEXTURE_2D, 0);
        }

        public int getCapacity() {
            return capacity;
        }
    }

    private record MaterialData(Material material) {
        public void use() {
            Texture diffuseTex = Texture.of(material.getDiffuseTex());
            if (diffuseTex != null)
                diffuseTex.bind();
        }
    }
}
