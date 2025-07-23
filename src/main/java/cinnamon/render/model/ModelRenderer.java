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
import java.util.*;

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

    protected static Pair<Integer, Integer> generateBuffers(List<VertexData> vertexData, Attributes... flags) {
        int vertexSize = Attributes.getVertexSize(flags);
        int capacity = vertexData.size() * vertexSize;

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
        public static final Vector3f DEFAULT_TANGENT = new Vector3f(0, 0, 1);
        public static final Vector3f DEFAULT_NORMAL = new Vector3f(0, 0, 1);
        public static final Vector2f DEFAULT_UV = new Vector2f(0, 0);

        public final Vector3f pos;
        public final Vector2f uv;
        public Vector3f tangent, norm;

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
            buffer.put(uv.x);
            buffer.put(1 - uv.y); //invert Y

            //push normal
            buffer.put(norm.x);
            buffer.put(norm.y);
            buffer.put(norm.z);

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

        public static void calculateFlatNormals(List<VertexData> list) {
            for (int i = 0; i < list.size(); i += 3) {
                VertexData v0 = list.get(i);
                VertexData v1 = list.get(i + 1);
                VertexData v2 = list.get(i + 2);

                //calculate normal vector
                //Dir = (B - A) x (C - A)
                //Norm = Dir / len(Dir)
                Vector3f normal = new Vector3f(v1.pos.x - v0.pos.x, v1.pos.y - v0.pos.y, v1.pos.z - v0.pos.z)
                        .cross(v2.pos.x - v0.pos.x, v2.pos.y - v0.pos.y, v2.pos.z - v0.pos.z).normalize();

                //set normal to the vertices
                v0.norm = normal;
                v1.norm = normal;
                v2.norm = normal;
            }
        }

        public static void smoothNormals(List<VertexData> list, float angleThreshold) {
            //find the smoothing groups based on the angle threshold
            List<List<Integer>> smoothingGroups = findSmoothingGroups(list, angleThreshold);
            Vector3f[] newNormals = new Vector3f[list.size()];

            //sum and smooth out the normals for each group
            for (List<Integer> group : smoothingGroups) {
                Vector3f accumulatedNormal = new Vector3f();
                for (int vertexIndex : group)
                    accumulatedNormal.add(list.get(vertexIndex).norm);
                accumulatedNormal.normalize();

                //update the normals for each vertex in the group
                for (int vertexIndex : group)
                    newNormals[vertexIndex] = accumulatedNormal;
            }

            //apply the new normals to the vertex data
            for (int i = 0; i < list.size(); i++)
                list.get(i).norm = newNormals[i];
        }

        public static void calculateTangents(List<VertexData> list, float angleThreshold) {
            //find the smoothing groups based on the angle threshold
            List<List<Integer>> smoothingGroups = findSmoothingGroups(list, angleThreshold);

            //calculate per-triangle tangents first
            List<Vector3f> triangleTangents = new ArrayList<>();
            for (int i = 0; i < list.size(); i += 3) {
                VertexData v0 = list.get(i);
                VertexData v1 = list.get(i + 1);
                VertexData v2 = list.get(i + 2);

                //calculate tangent vector
                Vector3f edge1 = new Vector3f(v1.pos).sub(v0.pos);
                Vector3f edge2 = new Vector3f(v2.pos).sub(v0.pos);

                float deltaU1 = v1.uv.x - v0.uv.x;
                float deltaV1 = v1.uv.y - v0.uv.y;
                float deltaU2 = v2.uv.x - v0.uv.x;
                float deltaV2 = v2.uv.y - v0.uv.y;

                float f = 1f / (deltaU1 * deltaV2 - deltaU2 * deltaV1);
                f = (Float.isInfinite(f) || Float.isNaN(f)) ? 0f : f;

                //the tangent vector
                triangleTangents.add(new Vector3f(
                        f * (deltaV2 * edge1.x - deltaV1 * edge2.x),
                        f * (deltaV2 * edge1.y - deltaV1 * edge2.y),
                        f * (deltaV2 * edge1.z - deltaV1 * edge2.z)
                ));
            }

            //now smooth the tangents based on the smoothing groups
            Vector3f[] newTangents = new Vector3f[list.size()];
            for (List<Integer> group : smoothingGroups) {
                //accumulate only if the triangle was not processed yet
                Vector3f accumulatedTangent = new Vector3f();
                Set<Integer> processedTriangles = new HashSet<>();

                for (int vertexIndex : group) {
                    int triangleIndex = vertexIndex / 3;
                    if (processedTriangles.add(triangleIndex))
                        accumulatedTangent.add(triangleTangents.get(triangleIndex));
                }

                //use the vertex normal to orthogonalize the tangent
                for (int vertexIndex : group) {
                    Vector3f t = new Vector3f(accumulatedTangent);
                    Vector3f n = list.get(vertexIndex).norm;

                    //if we had a valid tangent, orthogonalize it
                    if (t.lengthSquared() > 1e-6f) {
                        float dot = n.dot(t);
                        t.sub(n.x * dot, n.y * dot, n.z * dot).normalize();
                    } else {
                        //otherwise use the default tangent
                        t = DEFAULT_TANGENT;
                    }

                    newTangents[vertexIndex] = t;
                }
            }

            //apply the new tangents to the vertex data
            for (int i = 0; i < list.size(); i++)
                list.get(i).tangent = newTangents[i];
        }

        private static List<List<Integer>> findSmoothingGroups(List<VertexData> list, float angleThreshold) {
            //create a map of positions to indices
            Map<Vector3f, List<Integer>> posMap = new HashMap<>();
            for (int i = 0; i < list.size(); i++)
                posMap.computeIfAbsent(list.get(i).pos, k -> new ArrayList<>()).add(i);

            //create a list of smoothing groups and a visited array
            List<List<Integer>> smoothingGroups = new ArrayList<>();
            boolean[] visited = new boolean[list.size()];
            float cosThreshold = (float) Math.cos(Math.toRadians(angleThreshold));

            //now over all the vertices
            for (int i = 0; i < list.size(); i++) {
                //skip if already visited
                if (visited[i])
                    continue;

                //use BFS to find all connected vertices
                List<Integer> currentGroup = new ArrayList<>();
                Queue<Integer> toProcess = new LinkedList<>();
                toProcess.add(i);
                visited[i] = true;

                while (!toProcess.isEmpty()) {
                    //get the current index and add it to the group
                    int currentIndex = toProcess.poll();
                    currentGroup.add(currentIndex);

                    //get the potential matches for the current vertex position
                    List<Integer> potentialMatches = posMap.get(list.get(currentIndex).pos);
                    if (potentialMatches == null)
                        continue;

                    //for all the potential matches, check if they are connected, but only if not visited
                    for (int otherIndex : potentialMatches) {
                        if (!visited[otherIndex]) {
                            //check if the normals are within the angle threshold
                            if (Math.abs(list.get(currentIndex).norm.dot(list.get(otherIndex).norm)) >= cosThreshold - 1e-6f) {
                                //mark as visited and add to the queue
                                visited[otherIndex] = true;
                                toProcess.add(otherIndex);
                            }
                        }
                    }
                }

                //add the current group to the list
                smoothingGroups.add(currentGroup);
            }

            return smoothingGroups;
        }
    }
}
