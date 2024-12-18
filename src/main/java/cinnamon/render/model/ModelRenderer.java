package cinnamon.render.model;

import cinnamon.model.material.Material;
import cinnamon.render.MatrixStack;
import cinnamon.render.shader.Attributes;
import cinnamon.utils.AABB;
import cinnamon.utils.Maths;
import cinnamon.utils.Pair;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

public abstract class ModelRenderer {

    public abstract void free();

    public abstract void render(MatrixStack matrices);

    public abstract void render(MatrixStack matrices, Material material);

    public abstract void renderWithoutMaterial(MatrixStack matrices);

    public abstract AABB getAABB();

    public abstract List<AABB> getPreciseAABB();

    protected static Pair<Integer, Integer> generateBuffers(List<VertexData> vertexData) {
        Attributes[] flags = {Attributes.POS, Attributes.UV, Attributes.NORMAL, Attributes.TANGENTS};
        int capacity = vertexData.size() * (3 + 2 + 3 + 3); //pos, uv, norm, tangent
        int vertexSize = Attributes.getVertexSize(flags);

        //vao
        int vao = glGenVertexArrays();
        glBindVertexArray(vao);

        //vbo
        int vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, (long) capacity * Float.BYTES, GL_STATIC_DRAW);

        //load vertex attributes
        Attributes.load(flags, vertexSize);

        //enable attributes
        for (int i = 0; i < flags.length; i++)
            glEnableVertexAttribArray(i);

        //different buffer per group
        FloatBuffer buffer = BufferUtils.createFloatBuffer(capacity);

        //push vertices to buffer
        for (VertexData data : vertexData)
            data.pushToBuffer(buffer);

        //bind buffer to the current VBO
        buffer.rewind();
        glBufferSubData(GL_ARRAY_BUFFER, 0, buffer);

        return new Pair<>(vao, vbo);
    }

    protected static final class VertexData {
        public static final Vector3f DEFAULT_TANGENT = new Vector3f(0, 0, -1);

        public final Vector3f pos, norm;
        public final Vector2f uv;
        public Vector3f tangent;

        public VertexData(Vector3f pos, Vector2f uv, Vector3f norm) {
            this.pos = pos;
            this.uv = uv;
            this.norm = norm;
            this.tangent = DEFAULT_TANGENT;
        }

        public void pushToBuffer(FloatBuffer buffer) {
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

        public static List<VertexData> triangulate(List<VertexData> data) {
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

        public static void calculateTangents(List<VertexData> list) {
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
}
