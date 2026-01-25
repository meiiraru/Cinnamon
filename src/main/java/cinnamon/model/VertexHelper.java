package cinnamon.model;

import cinnamon.utils.Maths;
import cinnamon.utils.Pair;
import org.joml.Math;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.*;

public class VertexHelper {

    public static List<Vertex> triangulate(List<Vertex> data) {
        //list to return
        List<Vertex> triangles = new ArrayList<>();

        while (data.size() >= 3) {
            int n = data.size();
            boolean earFound = false;

            //iterate through all the vertices to find an ear
            for (int i = 0; i < n; i++) {
                int prev = (i - 1 + n) % n;
                int next = (i + 1) % n;

                //triangle
                Vertex a = data.get(prev);
                Vertex b = data.get(i);
                Vertex c = data.get(next);

                //check if the current triangle is an ear
                if (isEar(a.getPosition(), b.getPosition(), c.getPosition(), data)) {
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

    private static boolean isEar(Vector3f a, Vector3f b, Vector3f c, List<Vertex> list) {
        for (Vertex d : list) {
            Vector3f point = d.getPosition();

            //continue if the point is one of the triangle vertices
            if (point.equals(a) || point.equals(b) || point.equals(c))
                continue;

            //check if out point is inside the triangle
            if (Maths.isPointInTriangle(a, b, c, point))
                return false;
        }

        return true;
    }

    public static void calculateFlatNormals(List<Vertex> list) {
        for (int i = 0; i < list.size(); i += 3) {
            Vertex v0 = list.get(i);
            Vertex v1 = list.get(i + 1);
            Vertex v2 = list.get(i + 2);

            Vector3f A = v0.getPosition();
            Vector3f B = v1.getPosition();
            Vector3f C = v2.getPosition();

            //calculate normal vector
            //Dir = (B - A) x (C - A)
            //Norm = Dir / len(Dir)
            Vector3f normal = new Vector3f(B.x - A.x, B.y - A.y, B.z - A.z)
                    .cross(C.x - A.x, C.y - A.y, C.z - A.z).normalize();

            //set normal to the vertices
            v0.normal(normal);
            v1.normal(normal);
            v2.normal(normal);
        }
    }

    public static void smoothNormals(List<Vertex> list, float angleThreshold) {
        //find the smoothing groups based on the angle threshold
        List<List<Integer>> smoothingGroups = findSmoothingGroups(list, angleThreshold);
        Vector3f[] newNormals = new Vector3f[list.size()];

        //sum and smooth out the normals for each group
        for (List<Integer> group : smoothingGroups) {
            Vector3f accumulatedNormal = new Vector3f();
            for (int vertexIndex : group)
                accumulatedNormal.add(list.get(vertexIndex).getNormal());
            accumulatedNormal.normalize();

            //update the normals for each vertex in the group
            for (int vertexIndex : group)
                newNormals[vertexIndex] = accumulatedNormal;
        }

        //apply the new normals to the vertex data
        for (int i = 0; i < list.size(); i++)
            list.get(i).normal(newNormals[i]);
    }

    public static void calculateTangents(List<Vertex> list, float angleThreshold) {
        //find the smoothing groups based on the angle threshold
        List<List<Integer>> smoothingGroups = findSmoothingGroups(list, angleThreshold);

        //calculate per-triangle tangents first
        List<Vector3f> triangleTangents = new ArrayList<>();
        for (int i = 0; i < list.size(); i += 3) {
            Vertex v0 = list.get(i);
            Vertex v1 = list.get(i + 1);
            Vertex v2 = list.get(i + 2);

            //calculate tangent vector
            Vector3f A = v0.getPosition();
            Vector3f edge1 = new Vector3f(v1.getPosition()).sub(A);
            Vector3f edge2 = new Vector3f(v2.getPosition()).sub(A);

            Vector2f uv0 = v0.getUV();
            Vector2f uv1 = v1.getUV();
            Vector2f uv2 = v2.getUV();

            float deltaU1 = uv1.x - uv0.x;
            float deltaV1 = uv1.y - uv0.y;
            float deltaU2 = uv2.x - uv0.x;
            float deltaV2 = uv2.y - uv0.y;

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
                Vector3f n = list.get(vertexIndex).getNormal();

                //if we had a valid tangent, orthogonalize it
                if (t.lengthSquared() > 1e-6f) {
                    float dot = n.dot(t);
                    t.sub(n.x * dot, n.y * dot, n.z * dot).normalize();
                } else {
                    //otherwise use the default tangent
                    t = Vertex.DEFAULT_TANGENT;
                }

                newTangents[vertexIndex] = t;
            }
        }

        //apply the new tangents to the vertex data
        for (int i = 0; i < list.size(); i++)
            list.get(i).tangent(newTangents[i]);
    }

    private static List<List<Integer>> findSmoothingGroups(List<Vertex> list, float angleThreshold) {
        //create a map of positions to indices
        Map<Vector3f, List<Integer>> posMap = new HashMap<>();
        for (int i = 0; i < list.size(); i++)
            posMap.computeIfAbsent(list.get(i).getPosition(), k -> new ArrayList<>()).add(i);

        //create a list of smoothing groups and a visited array
        List<List<Integer>> smoothingGroups = new ArrayList<>();
        boolean[] visited = new boolean[list.size()];
        float cosThreshold = Math.cos(Math.toRadians(angleThreshold));

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
                List<Integer> potentialMatches = posMap.get(list.get(currentIndex).getPosition());
                if (potentialMatches == null)
                    continue;

                //for all the potential matches, check if they are connected, but only if not visited
                for (int otherIndex : potentialMatches) {
                    if (!visited[otherIndex]) {
                        //check if the normals are within the angle threshold
                        if (Math.abs(list.get(currentIndex).getNormal().dot(list.get(otherIndex).getNormal())) >= cosThreshold - 1e-6f) {
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

    public static Pair<int[], List<Vertex>> stripIndices(Collection<Vertex> vertices) {
        //prepare the new vertex list, also a map to remove duplicates and use as a lookup for indices
        List<Vertex> vertexList = new ArrayList<>();
        Map<Vertex, Integer> vertexToIndexMap = new HashMap<>();

        //the indices array
        int[] indices = new int[vertices.size()];
        int i = 0;

        //fill the indices array and the new vertex list
        for (Vertex vertex : vertices) {
            //try to find the vertex in the map
            Integer index = vertexToIndexMap.get(vertex);
            if (index == null) {
                //if not found, save vertex on the list, and save its index to the map
                index = vertexList.size();
                vertexToIndexMap.put(vertex, index);
                vertexList.add(vertex);
            }
            //set the index for the current vertex
            indices[i++] = index;
        }

        return new Pair<>(indices, vertexList);
    }

    public static void calculateUVs(Vector3f minBounds, Vector3f maxBounds, List<Vertex> list) {
        Vector3f size = new Vector3f(maxBounds).sub(minBounds);
        for (Vertex vertex : list) {
            Vector3f pos = vertex.getPosition();
            Vector3f norm = vertex.getNormal().absolute(new Vector3f());
            Vector2f uv = new Vector2f();
            if (norm.x >= norm.y && norm.x >= norm.z) {
                //x major
                uv.x = (pos.z - minBounds.z) / size.z;
                uv.y = (pos.y - minBounds.y) / size.y;
            } else if (norm.y >= norm.x && norm.y >= norm.z) {
                //y major
                uv.x = (pos.x - minBounds.x) / size.x;
                uv.y = (pos.z - minBounds.z) / size.z;
            } else {
                //z major
                uv.x = (pos.x - minBounds.x) / size.x;
                uv.y = (pos.y - minBounds.y) / size.y;
            }
            vertex.uv(uv);
        }
    }
}
