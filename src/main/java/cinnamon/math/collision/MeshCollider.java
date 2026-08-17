package cinnamon.math.collision;

import cinnamon.math.Maths;
import cinnamon.model.Vertex;
import cinnamon.model.VertexHelper;
import cinnamon.model.obj.Face;
import cinnamon.model.obj.Group;
import cinnamon.model.obj.Mesh;
import org.joml.Math;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class MeshCollider extends Collider<MeshCollider> {

    private static final Vector3f temp = new Vector3f();

    private final Vector3f[] vertices;
    private final Vector3f[] normals;
    private final AABB bounds;

    public MeshCollider(Mesh mesh) {
        List<Vector3f> verts = new ArrayList<>();
        List<Vector3f> norms = new ArrayList<>();
        List<Vector3f> meshVerts = mesh.getVertices();

        //bake the mesh into a triangle list for collision detection
        for (Group group : mesh.getGroups()) {
            for (Face face : group.getFaces()) {
                List<Integer> vIdx = face.getVertices();
                if (vIdx.size() < 3)
                    continue;

                //create the vertex data for the triangulation
                List<Vertex> data = new ArrayList<>();
                for (Integer idx : vIdx)
                    data.add(new Vertex().pos(meshVerts.get(idx)));

                //triangulate the face
                List<Vertex> sorted = VertexHelper.triangulate(data);

                //extract physical triangles
                for (int i = 0; i < sorted.size(); i += 3) {
                    Vector3f v0 = sorted.get(i).getPos();
                    Vector3f v1 = sorted.get(i + 1).getPos();
                    Vector3f v2 = sorted.get(i + 2).getPos();

                    verts.add(new Vector3f(v0));
                    verts.add(new Vector3f(v1));
                    verts.add(new Vector3f(v2));

                    //calculate and store the face normal
                    Vector3f n = new Vector3f(v1.x - v0.x, v1.y - v0.y, v1.z - v0.z).cross(v2.x - v0.x, v2.y - v0.y, v2.z - v0.z).normalize();
                    norms.add(n);
                }
            }
        }

        //wrap back into arrays
        this.vertices = verts.toArray(new Vector3f[0]);
        this.normals  = norms.toArray(new Vector3f[0]);

        //calculate the bounding box of the mesh
        this.bounds = new AABB();
        this.recalculateBounds();
    }

    public MeshCollider(MeshCollider mesh) {
        this(mesh.vertices, mesh.normals, mesh.bounds);
    }

    public MeshCollider(Vector3f[] vertices) {
        this(vertices, null);
    }

    public MeshCollider(Vector3f[] vertices, Vector3f[] normals) {
        this(vertices, normals, null);
    }

    public MeshCollider(Vector3f[] vertices, Vector3f[] normals, AABB bounds) {
        //clone vertices
        this.vertices = new Vector3f[vertices.length];
        for (int i = 0; i < vertices.length; i++)
            this.vertices[i] = new Vector3f(vertices[i]);

        //clone normals if they are 1/3 the length of vertices, otherwise, recalculate the normals
        if (normals != null && normals.length == vertices.length / 3) {
            this.normals = new Vector3f[normals.length];
            for (int i = 0; i < normals.length; i++) this.normals[i] = new Vector3f(normals[i]);
        } else {
            this.normals = new Vector3f[vertices.length / 3];
            for (int i = 0; i < vertices.length; i += 3) {
                Vector3f v0 = vertices[i];
                Vector3f v1 = vertices[i + 1];
                Vector3f v2 = vertices[i + 2];
                Vector3f n = new Vector3f(v1.x - v0.x, v1.y - v0.y, v1.z - v0.z).cross(v2.x - v0.x, v2.y - v0.y, v2.z - v0.z).normalize();
                this.normals[i / 3] = n;
            }
        }

        //clone bounds if not null, otherwise recalculate the bounds
        if (bounds != null) {
            this.bounds = bounds.clone();
        } else {
            this.bounds = new AABB();
            this.recalculateBounds();
        }
    }

    public MeshCollider set(MeshCollider mesh) {
        return this.set(mesh.vertices, mesh.normals, mesh.bounds);
    }

    public MeshCollider set(Vector3f[] vertices, Vector3f[] normals) {
        return this.set(vertices, normals, null);
    }

    public MeshCollider set(Vector3f[] vertices, Vector3f[] normals, AABB bounds) {
        if (vertices.length != this.vertices.length || normals.length != this.normals.length)
            throw new IllegalArgumentException("Vertex and normal arrays must match the original size");

        for (int i = 0; i < vertices.length; i++)
            this.vertices[i].set(vertices[i]);
        for (int i = 0; i < normals.length; i++)
            this.normals[i].set(normals[i]);

        if (bounds != null) {
            this.bounds.set(bounds);
        } else {
            this.recalculateBounds();
        }

        return this;
    }

    public Vector3f[] getVertices() {
        return vertices;
    }

    public Vector3f[] getNormals() {
        return normals;
    }

    public AABB getBounds() {
        return bounds;
    }

    protected void recalculateBounds() {
        if (vertices.length == 0)
            return;

        bounds.set(vertices[0], vertices[0]);
        for (Vector3f v : vertices)
            bounds.include(v);
    }

    @Override
    public MeshCollider clone() {
        return new MeshCollider(vertices, normals, bounds);
    }

    @Override
    public AABB toAABB() {
        return bounds.clone();
    }

    @Override
    public Vector3f getCenter() {
        return bounds.getCenter();
    }

    @Override
    public MeshCollider setCenter(float x, float y, float z) {
        Vector3f c = bounds.getCenter();
        return translate(x - c.x, y - c.y, z - c.z);
    }

    @Override
    public float getVolume() {
        return 0f; //undefined for arbitrary meshes
    }

    @Override
    public Vector3f getRandomPoint() {
        if (vertices.length == 0)
            return new Vector3f();

        //pick a random triangle
        int triIndex = (int) (Math.random() * (vertices.length / 3f)) * 3;
        Vector3f v0 = vertices[triIndex];
        Vector3f v1 = vertices[triIndex + 1];
        Vector3f v2 = vertices[triIndex + 2];

        //random barycentric coordinates
        float u = (float) Math.random();
        float v = (float) Math.random();
        if (u + v > 1f) {
            u = 1f - u;
            v = 1f - v;
        }
        float w = 1f - u - v;

        //return a random point inside the triangle
        return new Vector3f(v0.x * u + v1.x * v + v2.x * w, v0.y * u + v1.y * v + v2.y * w, v0.z * u + v1.z * v + v2.z * w);
    }

    @Override
    public MeshCollider translate(float x, float y, float z) {
        for (Vector3f v : vertices)
            v.add(x, y, z);
        bounds.translate(x, y, z);
        return this;
    }

    public MeshCollider rotate(Quaternionf rotation) {
        return this.rotate(rotation, getCenter());
    }

    public MeshCollider rotate(Quaternionf rotation, Vector3f anchor) {
        return this.rotate(rotation, anchor.x, anchor.y, anchor.z);
    }

    public MeshCollider rotate(Quaternionf rotation, float anchorX, float anchorY, float anchorZ) {
        for (Vector3f vertex : vertices)
            vertex.sub(anchorX, anchorY, anchorZ).rotate(rotation).add(anchorX, anchorY, anchorZ);
        for (Vector3f normal : normals)
            normal.rotate(rotation).normalize();
        recalculateBounds();
        return this;
    }

    public MeshCollider rotateX(float angle) {
        return this.rotateX(angle, getCenter());
    }

    public MeshCollider rotateX(float angle, Vector3f anchor) {
        return this.rotateX(angle, anchor.x, anchor.y, anchor.z);
    }

    public MeshCollider rotateX(float angle, float anchorX, float anchorY, float anchorZ) {
        float rad = Math.toRadians(angle);
        for (Vector3f vertex : vertices)
            vertex.sub(anchorX, anchorY, anchorZ).rotateX(rad).add(anchorX, anchorY, anchorZ);
        for (Vector3f normal : normals)
            normal.rotateX(rad).normalize();
        recalculateBounds();
        return this;
    }

    public MeshCollider rotateY(float angle) {
        return this.rotateY(angle, getCenter());
    }

    public MeshCollider rotateY(float angle, Vector3f anchor) {
        return this.rotateY(angle, anchor.x, anchor.y, anchor.z);
    }

    public MeshCollider rotateY(float angle, float anchorX, float anchorY, float anchorZ) {
        float rad = Math.toRadians(angle);
        for (Vector3f vertex : vertices)
            vertex.sub(anchorX, anchorY, anchorZ).rotateY(rad).add(anchorX, anchorY, anchorZ);
        for (Vector3f normal : normals)
            normal.rotateY(rad).normalize();
        recalculateBounds();
        return this;
    }

    public MeshCollider rotateZ(float angle) {
        return this.rotateZ(angle, getCenter());
    }

    public MeshCollider rotateZ(float angle, Vector3f anchor) {
        return this.rotateZ(angle, anchor.x, anchor.y, anchor.z);
    }

    public MeshCollider rotateZ(float angle, float anchorX, float anchorY, float anchorZ) {
        float rad = Math.toRadians(angle);
        for (Vector3f vertex : vertices)
            vertex.sub(anchorX, anchorY, anchorZ).rotateZ(rad).add(anchorX, anchorY, anchorZ);
        for (Vector3f normal : normals)
            normal.rotateZ(rad).normalize();
        recalculateBounds();
        return this;
    }

    @Override
    public MeshCollider applyMatrix(Matrix4f matrix) {
        if ((matrix.properties() & Matrix4f.PROPERTY_IDENTITY) != 0)
            return this;

        for (Vector3f vertex : vertices)
            vertex.mulPosition(matrix);

        Matrix4f inv = new Matrix4f(matrix).invert().transpose();
        for (Vector3f normal : normals)
            inv.transformDirection(normal).normalize();

        recalculateBounds();
        return this;
    }

    @Override
    public boolean containsPoint(float x, float y, float z) {
        if (!bounds.containsPoint(x, y, z))
            return false;

        for (int i = 0; i < vertices.length; i += 3) {
            closestPointOnTriangle(x, y, z, vertices[i], vertices[i + 1], vertices[i + 2], temp);
            if (temp.distanceSquared(x, y, z) <= Maths.KINDA_SMALL_NUMBER)
                return true;
        }
        return false;
    }

    @Override
    public float distanceToPoint(float x, float y, float z) {
        if (!bounds.containsPoint(x, y, z))
            return bounds.distanceToPoint(x, y, z);

        float minDistSq = Float.MAX_VALUE;
        for (int i = 0; i < vertices.length; i += 3) {
            closestPointOnTriangle(x, y, z, vertices[i], vertices[i + 1], vertices[i + 2], temp);
            float distSq = temp.distanceSquared(x, y, z);
            if (distSq < minDistSq)
                minDistSq = distSq;
        }

        return Math.sqrt(minDistSq);
    }

    @Override
    public Vector3f closestPoint(float x, float y, float z, Vector3f out) {
        if (!bounds.containsPoint(x, y, z))
            return bounds.closestPoint(x, y, z, out);

        //if inside the broadphase bounds, find the actual closest triangle
        float minDistSq = Float.MAX_VALUE;
        for (int i = 0; i < vertices.length; i += 3) {
            closestPointOnTriangle(x, y, z, vertices[i], vertices[i + 1], vertices[i + 2], temp);
            float distSq = temp.distanceSquared(x, y, z);
            if (distSq < minDistSq) {
                minDistSq = distSq;
                out.set(temp);
            }
        }

        return out;
    }

    @Override
    public boolean intersects(Sphere sphere) {
        if (!bounds.intersects(sphere))
            return false;

        float rSq = sphere.getRadius() * sphere.getRadius();
        Vector3f spCenter = sphere.getCenter();

        for (int i = 0; i < vertices.length; i += 3) {
            closestPointOnTriangle(spCenter.x, spCenter.y, spCenter.z, vertices[i], vertices[i + 1], vertices[i + 2], temp);
            if (spCenter.distanceSquared(temp) <= rSq)
                return true;
        }

        return false;
    }

    @Override
    public boolean intersects(AABB aabb) {
        return collide(aabb) != null;
    }

    @Override
    public boolean intersects(OBB obb) {
        return collide(obb) != null;
    }

    @Override
    public boolean intersects(Plane plane) {
        if (!bounds.intersects(plane))
            return false;

        for (Vector3f v : vertices) {
            if (plane.getNormal().dot(v) - plane.getDistance() <= 0)
                return true;
        }

        return false;
    }

    @Override
    public boolean intersects(MeshCollider mesh) {
        if (!bounds.intersects(mesh.bounds))
            return false;

        TriangleProxy proxyA = new TriangleProxy();
        TriangleProxy proxyB = new TriangleProxy();

        for (int i = 0; i < vertices.length; i += 3) {
            proxyA.set(vertices[i], vertices[i + 1], vertices[i + 2], normals[i / 3]);

            for (int j = 0; j < mesh.vertices.length; j += 3) {
                //overlap check between two triangles
                if (!trianglesOverlap(vertices[i], vertices[i + 1], vertices[i + 2], mesh.vertices[j], mesh.vertices[j + 1], mesh.vertices[j + 2]))
                    continue;

                proxyB.set(mesh.vertices[j], mesh.vertices[j + 1], mesh.vertices[j + 2], mesh.normals[j / 3]);

                if (SATHelper.SATCollide(proxyA, proxyB, proxyA.axes, proxyB.axes) != null)
                    return true;
            }
        }

        return false;
    }

    @Override
    public Hit rayCast(Ray ray) {
        if (bounds.rayCast(ray) == null)
            return null;

        Hit closestHit = null;
        for (int i = 0; i < vertices.length; i += 3) {
            Hit hit = rayTriangle(ray, vertices[i], vertices[i + 1], vertices[i + 2], normals[i / 3], this);
            if (hit != null && (closestHit == null || hit.tNear() < closestHit.tNear()))
                closestHit = hit;
        }

        return closestHit;
    }

    @Override
    public Collision collide(Sphere sphere) {
        if (!bounds.intersects(sphere))
            return null;

        Collision bestCol = null;
        float rsq = sphere.getRadius() * sphere.getRadius();
        Vector3f spCenter = sphere.getCenter();

        for (int i = 0; i < vertices.length; i += 3) {
            closestPointOnTriangle(spCenter.x, spCenter.y, spCenter.z, vertices[i], vertices[i + 1], vertices[i + 2], temp);
            float distSq = spCenter.distanceSquared(temp);

            if (distSq <= rsq) {
                float dist = Math.sqrt(distSq);
                float depth = sphere.getRadius() - dist;
                Vector3f colNormal = new Vector3f();
                if (dist > Maths.KINDA_SMALL_NUMBER) {
                    colNormal.set((spCenter.x - temp.x) / dist, (spCenter.y - temp.y) / dist, (spCenter.z - temp.z) / dist);
                } else {
                    colNormal.set(normals[i / 3]);
                }

                if (bestCol == null || depth > bestCol.depth())
                    bestCol = new Collision(colNormal, depth, this, sphere);
            }
        }

        return bestCol;
    }

    @Override
    public Collision collide(AABB aabb) {
        if (!bounds.intersects(aabb))
            return null;

        TriangleProxy proxy = new TriangleProxy();
        Collision bestCol = null;

        for (int i = 0; i < vertices.length; i += 3) {
            proxy.set(vertices[i], vertices[i + 1], vertices[i + 2], normals[i / 3]);
            Collision col = SATHelper.SATCollide(proxy, aabb, proxy.axes, SATHelper.AABB_AXES);

            if (col != null && (bestCol == null || col.depth() > bestCol.depth()))
                bestCol = new Collision(col.normal(), col.depth(), this, aabb);
        }

        return bestCol;
    }

    @Override
    public Collision collide(OBB obb) {
        if (!bounds.intersects(obb))
            return null;

        TriangleProxy proxy = new TriangleProxy();
        Vector3f[] obbAxes = new Vector3f[]{obb.getAxisX(), obb.getAxisY(), obb.getAxisZ()};
        Collision bestCol = null;

        for (int i = 0; i < vertices.length; i += 3) {
            proxy.set(vertices[i], vertices[i + 1], vertices[i + 2], normals[i / 3]);
            Collision col = SATHelper.SATCollide(proxy, obb, proxy.axes, obbAxes);

            if (col != null && (bestCol == null || col.depth() > bestCol.depth()))
                bestCol = new Collision(col.normal(), col.depth(), this, obb);
        }

        return bestCol;
    }

    @Override
    public Collision collide(Plane plane) {
        if (!bounds.intersects(plane))
            return null;

        float maxDepth = -Float.MAX_VALUE;
        Vector3f colNorm = new Vector3f(plane.getNormal()).negate();

        for (Vector3f v : vertices) {
            float d = plane.getNormal().dot(v) - plane.getDistance();
            if (d <= 0) {
                float depth = -d;
                if (depth > maxDepth)
                    maxDepth = depth;
            }
        }

        return maxDepth >= 0 ? new Collision(colNorm, maxDepth, this, plane) : null;
    }

    @Override
    public Collision collide(MeshCollider mesh) {
        if (!bounds.intersects(mesh.bounds))
            return null;

        TriangleProxy proxyA = new TriangleProxy();
        TriangleProxy proxyB = new TriangleProxy();
        Collision bestCol = null;

        for (int i = 0; i < vertices.length; i += 3) {
            proxyA.set(vertices[i], vertices[i + 1], vertices[i + 2], normals[i / 3]);

            for (int j = 0; j < mesh.vertices.length; j += 3) {
                if (!trianglesOverlap(vertices[i], vertices[i + 1], vertices[i + 2], mesh.vertices[j], mesh.vertices[j + 1], mesh.vertices[j + 2]))
                    continue;

                proxyB.set(mesh.vertices[j], mesh.vertices[j + 1], mesh.vertices[j + 2], mesh.normals[j / 3]);
                Collision col = SATHelper.SATCollide(proxyA, proxyB, proxyA.axes, proxyB.axes);

                if (col != null && (bestCol == null || col.depth() > bestCol.depth()))
                    bestCol = new Collision(col.normal(), col.depth(), this, mesh);
            }
        }

        return bestCol;
    }

    @Override
    public Hit sweep(Sphere sphere, Vector3f velocity) {
        Hit boundsHit = bounds.sweep(sphere, velocity);
        if (boundsHit == null && !bounds.intersects(sphere))
            return null;

        TriangleProxy proxy = new TriangleProxy();
        Hit bestHit = null;

        for (int i = 0; i < vertices.length; i += 3) {
            proxy.set(vertices[i], vertices[i + 1], vertices[i + 2], normals[i / 3]);
            Hit hit = proxy.sweep(sphere, velocity);

            if (hit != null && (bestHit == null || hit.tNear() < bestHit.tNear()))
                bestHit = new Hit(hit.position(), hit.normal(), hit.tNear(), hit.tFar(), hit.ray(), this);
        }

        return bestHit;
    }

    @Override
    public Hit sweep(AABB aabb, Vector3f velocity) {
        return sweepProxySAT(aabb, velocity, SATHelper.AABB_AXES);
    }

    @Override
    public Hit sweep(OBB obb, Vector3f velocity) {
        return sweepProxySAT(obb, velocity, new Vector3f[]{obb.getAxisX(), obb.getAxisY(), obb.getAxisZ()});
    }

    @Override
    public Hit sweep(Plane plane, Vector3f velocity) {
        //plane is infinite, we can sweep the bounds as a highly accurate proxy
        Hit hit = bounds.sweep(plane, velocity);
        if (hit != null) hit.setCollider(this);
        return hit;
    }

    @Override
    public Hit sweep(MeshCollider mesh, Vector3f velocity) {
        if (bounds.sweep(mesh.bounds, velocity) == null && !bounds.intersects(mesh.bounds))
            return null;

        TriangleProxy proxyA = new TriangleProxy();
        TriangleProxy proxyB = new TriangleProxy();
        Hit bestHit = null;

        for (int i = 0; i < vertices.length; i += 3) {
            proxyA.set(vertices[i], vertices[i + 1], vertices[i + 2], normals[i / 3]);

            for (int j = 0; j < mesh.vertices.length; j += 3) {
                if (!trianglesSweepOverlap(vertices[i], vertices[i + 1], vertices[i + 2], velocity, mesh.vertices[j], mesh.vertices[j + 1], mesh.vertices[j + 2]))
                    continue;

                proxyB.set(mesh.vertices[j], mesh.vertices[j + 1], mesh.vertices[j + 2], mesh.normals[j / 3]);
                Hit hit = SATHelper.SATSweep(proxyA, proxyB, proxyA.axes, proxyB.axes, velocity);

                if (hit != null && (bestHit == null || hit.tNear() < bestHit.tNear()))
                    bestHit = new Hit(hit.position(), hit.normal(), hit.tNear(), hit.tFar(), hit.ray(), this);
            }
        }

        return bestHit;
    }

    protected Hit sweepProxySAT(Collider<?> shape, Vector3f velocity, Vector3f[] shapeAxes) {
        Hit boundsHit = bounds.sweep(shape, velocity);
        if (boundsHit == null && !bounds.intersects(shape))
            return null;

        TriangleProxy proxy = new TriangleProxy();
        Hit bestHit = null;

        for (int i = 0; i < vertices.length; i += 3) {
            proxy.set(vertices[i], vertices[i + 1], vertices[i + 2], normals[i / 3]);
            Hit hit = SATHelper.SATSweep(proxy, shape, proxy.axes, shapeAxes, velocity);

            if (hit != null && (bestHit == null || hit.tNear() < bestHit.tNear()))
                bestHit = new Hit(hit.position(), hit.normal(), hit.tNear(), hit.tFar(), hit.ray(), this);
        }

        return bestHit;
    }

    @Override
    public void project(Vector3f axis, float[] minMax) {
        bounds.project(axis, minMax);
    }

    protected static Hit rayTriangle(Ray ray, Vector3f v0, Vector3f v1, Vector3f v2, Vector3f normal, Collider<?> hitCollider) {
        Vector3f dir = ray.getDirection();

        //edge1, edge 2 = v1, v2 - v0
        float e1x = v1.x - v0.x, e1y = v1.y - v0.y, e1z = v1.z - v0.z;
        float e2x = v2.x - v0.x, e2y = v2.y - v0.y, e2z = v2.z - v0.z;

        //h = dir cross edge2
        float hx = dir.y * e2z - dir.z * e2y;
        float hy = dir.z * e2x - dir.x * e2z;
        float hz = dir.x * e2y - dir.y * e2x;

        //a = edge1 dot h
        float a = e1x * hx + e1y * hy + e1z * hz;

        if (a > -Maths.KINDA_SMALL_NUMBER && a < Maths.KINDA_SMALL_NUMBER)
            return null;

        float f = 1f / a;
        Vector3f orig = ray.getOrigin();

        //s = orig - v0
        float sx = orig.x - v0.x, sy = orig.y - v0.y, sz = orig.z - v0.z;
        //u = f * (s dot h)
        float u = f * (sx * hx + sy * hy + sz * hz);
        if (u < 0f || u > 1f)
            return null;

        //q = s cross edge1
        float qx = sy * e1z - sz * e1y;
        float qy = sz * e1x - sx * e1z;
        float qz = sx * e1y - sy * e1x;

        //v = f * (dir dot q)
        float v = f * (dir.x * qx + dir.y * qy + dir.z * qz);
        if (v < 0f || u + v > 1f)
            return null;

        //t = f * (edge2 dot q)
        float t = f * (e2x * qx + e2y * qy + e2z * qz);
        if (t <= Maths.KINDA_SMALL_NUMBER || t > ray.getMaxDistance())
            return null;

        //return the hit results
        Vector3f hitPos = new Vector3f(ray.getOrigin()).fma(t, ray.getDirection());
        Vector3f hitNorm = new Vector3f(normal);
        if (a < 0f) hitNorm.negate();

        float normT = t / ray.getMaxDistance();
        return new Hit(hitPos, hitNorm, normT, normT, ray, hitCollider);
    }

    protected static void closestPointOnTriangle(float px, float py, float pz, Vector3f a, Vector3f b, Vector3f c, Vector3f out) {
        //ab, ac, ap = b, c, p - a
        float abx = b.x - a.x, aby = b.y - a.y, abz = b.z - a.z;
        float acx = c.x - a.x, acy = c.y - a.y, acz = c.z - a.z;
        float apx = px  - a.x, apy = py  - a.y, apz = pz  - a.z;

        //d1, d2 = ab, ac dot ap
        float d1 = abx * apx + aby * apy + abz * apz;
        float d2 = acx * apx + acy * apy + acz * apz;
        if (d1 <= 0f && d2 <= 0f) {
            out.set(a);
            return;
        }

        //bp = p - b
        float bpx = px - b.x, bpy = py - b.y, bpz = pz - b.z;

        //d3, d4 = ab, ac dot bp
        float d3 = abx * bpx + aby * bpy + abz * bpz;
        float d4 = acx * bpx + acy * bpy + acz * bpz;
        if (d3 >= 0f && d4 <= d3) {
            out.set(b);
            return;
        }

        float vc = d1 * d4 - d3 * d2;
        if (vc <= 0f && d1 >= 0f && d3 <= 0f) {
            float v = d1 / (d1 - d3);
            out.set(a.x + abx * v, a.y + aby * v, a.z + abz * v);
            return;
        }

        //cp = p - c
        float cpx = px - c.x, cpy = py - c.y, cpz = pz - c.z;

        //d5, d6 = ab, ac dot cp
        float d5 = abx * cpx + aby * cpy + abz * cpz;
        float d6 = acx * cpx + acy * cpy + acz * cpz;
        if (d6 >= 0f && d5 <= d6) {
            out.set(c);
            return;
        }

        float vb = d5 * d2 - d1 * d6;
        if (vb <= 0f && d2 >= 0f && d6 <= 0f) {
            float w = d2 / (d2 - d6);
            out.set(a.x + acx * w, a.y + acy * w, a.z + acz * w);
            return;
        }

        float va = d3 * d6 - d5 * d4;
        if (va <= 0f && (d4 - d3) >= 0f && (d5 - d6) >= 0f) {
            float w = (d4 - d3) / ((d4 - d3) + (d5 - d6));
            out.set(b.x + (c.x - b.x) * w, b.y + (c.y - b.y) * w, b.z + (c.z - b.z) * w);
            return;
        }

        float denom = 1f / (va + vb + vc);
        float v = vb * denom;
        float w = vc * denom;
        out.set(a.x + abx * v + acx * w, a.y + aby * v + acy * w, a.z + abz * v + acz * w);
    }

    protected static boolean trianglesOverlap(Vector3f a0, Vector3f a1, Vector3f a2, Vector3f b0, Vector3f b1, Vector3f b2) {
        if (Math.max(a0.x, Math.max(a1.x, a2.x)) < Math.min(b0.x, Math.min(b1.x, b2.x))) return false;
        if (Math.min(a0.x, Math.min(a1.x, a2.x)) > Math.max(b0.x, Math.max(b1.x, b2.x))) return false;
        if (Math.max(a0.y, Math.max(a1.y, a2.y)) < Math.min(b0.y, Math.min(b1.y, b2.y))) return false;
        if (Math.min(a0.y, Math.min(a1.y, a2.y)) > Math.max(b0.y, Math.max(b1.y, b2.y))) return false;
        if (Math.max(a0.z, Math.max(a1.z, a2.z)) < Math.min(b0.z, Math.min(b1.z, b2.z))) return false;
        if (Math.min(a0.z, Math.min(a1.z, a2.z)) > Math.max(b0.z, Math.max(b1.z, b2.z))) return false;
        return true;
    }

    protected static boolean trianglesSweepOverlap(Vector3f a0, Vector3f a1, Vector3f a2, Vector3f vel, Vector3f b0, Vector3f b1, Vector3f b2) {
        float minAX = Math.min(a0.x, Math.min(a1.x, a2.x));
        float maxAX = Math.max(a0.x, Math.max(a1.x, a2.x));
        if (vel.x < 0) minAX += vel.x;
        else maxAX += vel.x;
        float minBX = Math.min(b0.x, Math.min(b1.x, b2.x));
        float maxBX = Math.max(b0.x, Math.max(b1.x, b2.x));
        if (maxAX < minBX || minAX > maxBX)
            return false;

        float minAY = Math.min(a0.y, Math.min(a1.y, a2.y));
        float maxAY = Math.max(a0.y, Math.max(a1.y, a2.y));
        if (vel.y < 0) minAY += vel.y;
        else maxAY += vel.y;
        float minBY = Math.min(b0.y, Math.min(b1.y, b2.y));
        float maxBY = Math.max(b0.y, Math.max(b1.y, b2.y));
        if (maxAY < minBY || minAY > maxBY)
            return false;

        float minAZ = Math.min(a0.z, Math.min(a1.z, a2.z));
        float maxAZ = Math.max(a0.z, Math.max(a1.z, a2.z));
        if (vel.z < 0) minAZ += vel.z;
        else maxAZ += vel.z;
        float minBZ = Math.min(b0.z, Math.min(b1.z, b2.z));
        float maxBZ = Math.max(b0.z, Math.max(b1.z, b2.z));
        if (maxAZ < minBZ || minAZ > maxBZ)
            return false;

        return true;
    }

    protected static class TriangleProxy extends Collider<TriangleProxy> {
        final Vector3f v0 = new Vector3f(), v1 = new Vector3f(), v2 = new Vector3f(), center = new Vector3f();
        final Vector3f[] axes = new Vector3f[]{new Vector3f(), new Vector3f(), new Vector3f(), new Vector3f()};

        void set(Vector3f a, Vector3f b, Vector3f c, Vector3f normal) {
            v0.set(a);
            v1.set(b);
            v2.set(c);
            center.set((a.x + b.x + c.x) / 3f, (a.y + b.y + c.y) / 3f, (a.z + b.z + c.z) / 3f);

            axes[0].set(normal);
            axes[1].set(v1.x - v0.x, v1.y - v0.y, v1.z - v0.z).normalize();
            axes[2].set(v2.x - v1.x, v2.y - v1.y, v2.z - v1.z).normalize();
            axes[3].set(v0.x - v2.x, v0.y - v2.y, v0.z - v2.z).normalize();
        }

        @Override
        public void project(Vector3f axis, float[] minMax) {
            float p0 = v0.dot(axis), p1 = v1.dot(axis), p2 = v2.dot(axis);
            minMax[0] = Math.min(p0, Math.min(p1, p2));
            minMax[1] = Math.max(p0, Math.max(p1, p2));
        }

        @Override
        public Vector3f getCenter() {
            return center;
        }

        @Override
        public Vector3f closestPoint(float x, float y, float z, Vector3f out) {
            closestPointOnTriangle(x, y, z, v0, v1, v2, out);
            return out;
        }

        @Override
        public TriangleProxy clone() {
            TriangleProxy t = new TriangleProxy();
            t.set(v0, v1, v2, axes[0]);
            return t;
        }

        @Override
        public TriangleProxy translate(float x, float y, float z) {
            v0.add(x, y, z);
            v1.add(x, y, z);
            v2.add(x, y, z);
            center.add(x, y, z);
            return this;
        }

        @Override
        public Hit sweep(Sphere sphere, Vector3f velocity) {
            //simplified sweep for individual proxy triangles against a sphere
            Hit hit = rayTriangle(new Ray(sphere.getCenter(), velocity, velocity.length()), v0, v1, v2, axes[0], this);
            if (hit == null)
                return null;

            hit.position().sub(new Vector3f(velocity).normalize().mul(sphere.getRadius()));
            return hit;
        }

        @Override
        public AABB toAABB() {
            throw new UnsupportedOperationException();
        }

        @Override
        public TriangleProxy setCenter(float x, float y, float z) {
            return this;
        }

        @Override
        public float getVolume() {
            return 0;
        }
        @Override
        public Vector3f getRandomPoint() {
            return null;
        }
        @Override
        public TriangleProxy applyMatrix(Matrix4f matrix) {
            return this;
        }
        @Override
        public boolean containsPoint(float x, float y, float z) {
            return false;
        }
        @Override
        public float distanceToPoint(float x, float y, float z) {
            return 0;
        }

        @Override
        public boolean intersects(Sphere sphere) {
            return false;
        }
        @Override
        public boolean intersects(AABB aabb) {
            return false;
        }
        @Override
        public boolean intersects(OBB obb) {
            return false;
        }
        @Override
        public boolean intersects(Plane plane) {
            return false;
        }
        @Override
        public boolean intersects(MeshCollider mesh) {
            return false;
        }
        @Override
        public Hit rayCast(Ray ray) {
            return null;
        }
        @Override
        public Collision collide(Sphere sphere) {
            return null;
        }
        @Override
        public Collision collide(AABB aabb) {
            return null;
        }
        @Override
        public Collision collide(OBB obb) {
            return null;
        }
        @Override
        public Collision collide(Plane plane) {
            return null;
        }
        @Override
        public Collision collide(MeshCollider mesh) {
            return null;
        }
        @Override
        public Hit sweep(AABB aabb, Vector3f velocity) {
            return null;
        }
        @Override
        public Hit sweep(OBB obb, Vector3f velocity) {
            return null;
        }
        @Override
        public Hit sweep(Plane plane, Vector3f velocity) {
            return null;
        }
        @Override
        public Hit sweep(MeshCollider mesh, Vector3f velocity) {
            return null;
        }
    }
}