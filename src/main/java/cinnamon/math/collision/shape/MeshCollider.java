package cinnamon.math.collision.shape;

import cinnamon.math.collision.Collider;
import cinnamon.math.collision.Collision;
import cinnamon.math.collision.Hit;
import cinnamon.math.collision.Ray;
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

    private final Triangle[] triangles;
    private final AABB bounds;

    public MeshCollider(Mesh mesh) {
        List<Triangle> tris = new ArrayList<>();
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
                for (int i = 0; i < sorted.size(); i += 3)
                    tris.add(new Triangle(sorted.get(i).getPos(), sorted.get(i + 1).getPos(), sorted.get(i + 2).getPos()));
            }
        }

        //wrap back into an array and calculate the bounding box of the mesh
        this.triangles = tris.toArray(new Triangle[0]);
        this.bounds = new AABB();
        this.recalculateBounds();
    }

    public MeshCollider(MeshCollider mesh) {
        this.triangles = new Triangle[mesh.triangles.length];
        for (int i = 0; i < mesh.triangles.length; i++)
            this.triangles[i] = mesh.triangles[i].clone();
        this.bounds = mesh.bounds.clone();
    }

    public MeshCollider(Vector3f[] vertices) {
        this(vertices, null);
    }

    public MeshCollider(Vector3f[] vertices, Vector3f[] normals) {
        this(vertices, normals, null);
    }

    public MeshCollider(Vector3f[] vertices, Vector3f[] normals, AABB bounds) {
        //use normals only if they are 1/3 the length of vertices, otherwise, recalculate the normals
        boolean validNormals = normals != null && normals.length == vertices.length / 3;

        //create triangles
        this.triangles = new Triangle[vertices.length / 3];
        for (int i = 0; i < vertices.length; i += 3) {
            Triangle t = new Triangle();
            if (validNormals) {
                t.set(vertices[i], vertices[i + 1], vertices[i + 2], normals[i / 3]);
            } else {
                t.set(vertices[i], vertices[i + 1], vertices[i + 2]);
            }
            this.triangles[i / 3] = t;
        }

        //clone bounds if not null, otherwise recalculate the bounds
        if (bounds != null) {
            this.bounds = bounds.clone();
        } else {
            this.bounds = new AABB();
            this.recalculateBounds();
        }
    }

    public Triangle[] getTriangles() {
        return triangles;
    }

    public AABB getBounds() {
        return bounds;
    }

    protected void recalculateBounds() {
        if (triangles.length == 0)
            return;

        bounds.set(triangles[0].v0, triangles[0].v0);
        for (Triangle tri : triangles) {
            bounds.include(tri.v0);
            bounds.include(tri.v1);
            bounds.include(tri.v2);
        }
    }

    @Override
    public MeshCollider clone() {
        return new MeshCollider(this);
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
        return 0f;
    }

    @Override
    public Vector3f getRandomPoint(Vector3f out) {
        if (triangles.length == 0)
            return out;

        int triIndex = (int) (Math.random() * triangles.length);
        return triangles[triIndex].getRandomPoint(out);
    }

    @Override
    public MeshCollider translate(float x, float y, float z) {
        for (Triangle tri : triangles)
            tri.translate(x, y, z);
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
        for (Triangle tri : triangles) {
            tri.v0.sub(anchorX, anchorY, anchorZ).rotate(rotation).add(anchorX, anchorY, anchorZ);
            tri.v1.sub(anchorX, anchorY, anchorZ).rotate(rotation).add(anchorX, anchorY, anchorZ);
            tri.v2.sub(anchorX, anchorY, anchorZ).rotate(rotation).add(anchorX, anchorY, anchorZ);
            tri.set(tri.v0, tri.v1, tri.v2, tri.getNormal().rotate(rotation).normalize());
        }
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
        for (Triangle tri : triangles) {
            tri.v0.sub(anchorX, anchorY, anchorZ).rotateX(rad).add(anchorX, anchorY, anchorZ);
            tri.v1.sub(anchorX, anchorY, anchorZ).rotateX(rad).add(anchorX, anchorY, anchorZ);
            tri.v2.sub(anchorX, anchorY, anchorZ).rotateX(rad).add(anchorX, anchorY, anchorZ);
            tri.set(tri.v0, tri.v1, tri.v2, tri.getNormal().rotateX(rad).normalize());
        }
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
        for (Triangle tri : triangles) {
            tri.v0.sub(anchorX, anchorY, anchorZ).rotateY(rad).add(anchorX, anchorY, anchorZ);
            tri.v1.sub(anchorX, anchorY, anchorZ).rotateY(rad).add(anchorX, anchorY, anchorZ);
            tri.v2.sub(anchorX, anchorY, anchorZ).rotateY(rad).add(anchorX, anchorY, anchorZ);
            tri.set(tri.v0, tri.v1, tri.v2, tri.getNormal().rotateY(rad).normalize());
        }
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
        for (Triangle tri : triangles) {
            tri.v0.sub(anchorX, anchorY, anchorZ).rotateZ(rad).add(anchorX, anchorY, anchorZ);
            tri.v1.sub(anchorX, anchorY, anchorZ).rotateZ(rad).add(anchorX, anchorY, anchorZ);
            tri.v2.sub(anchorX, anchorY, anchorZ).rotateZ(rad).add(anchorX, anchorY, anchorZ);
            tri.set(tri.v0, tri.v1, tri.v2, tri.getNormal().rotateZ(rad).normalize());
        }
        recalculateBounds();
        return this;
    }

    @Override
    public MeshCollider applyMatrix(Matrix4f matrix) {
        if ((matrix.properties() & Matrix4f.PROPERTY_IDENTITY) != 0)
            return this;

        for (Triangle tri : triangles)
            tri.applyMatrix(matrix);

        recalculateBounds();
        return this;
    }

    @Override
    public boolean containsPoint(float x, float y, float z) {
        if (!bounds.containsPoint(x, y, z))
            return false;

        for (Triangle tri : triangles) {
            if (tri.containsPoint(x, y, z))
                return true;
        }

        return false;
    }

    @Override
    public float distanceToPoint(float x, float y, float z) {
        if (!bounds.containsPoint(x, y, z))
            return bounds.distanceToPoint(x, y, z);

        float minDist = Float.MAX_VALUE;
        for (Triangle tri : triangles) {
            float dist = tri.distanceToPoint(x, y, z);
            if (dist < minDist)
                minDist = dist;
        }

        return minDist;
    }

    @Override
    public Vector3f closestPoint(float x, float y, float z, Vector3f out) {
        if (!bounds.containsPoint(x, y, z))
            return bounds.closestPoint(x, y, z, out);

        //if inside the broadphase bounds, find the actual closest triangle
        float minDistSq = Float.MAX_VALUE;
        float ox = 0f, oy = 0f, oz = 0f;

        for (Triangle tri : triangles) {
            tri.closestPoint(x, y, z, out);
            float distSq = out.distanceSquared(x, y, z);
            if (distSq < minDistSq) {
                minDistSq = distSq;
                ox = out.x;
                oy = out.y;
                oz = out.z;
            }
        }

        return out.set(ox, oy, oz);
    }

    @Override
    public void project(Vector3f axis, float[] minMax) {
        bounds.project(axis, minMax);
    }

    @Override
    public boolean intersects(Sphere sphere) {
        return genericIntersects(sphere);
    }

    @Override
    public boolean intersects(AABB aabb) {
        return genericIntersects(aabb);
    }

    @Override
    public boolean intersects(OBB obb) {
        return genericIntersects(obb);
    }

    @Override
    public boolean intersects(Plane plane) {
        return genericIntersects(plane);
    }

    @Override
    public boolean intersects(Triangle triangle) {
        return genericIntersects(triangle);
    }

    @Override
    public boolean intersects(MeshCollider mesh) {
        if (!bounds.intersects(mesh.bounds))
            return false;

        for (Triangle triA : triangles) {
            for (Triangle triB : mesh.triangles) {
                if (triA.intersects(triB))
                    return true;
            }
        }

        return false;
    }

    protected boolean genericIntersects(Collider<?> shape) {
        if (!bounds.intersects(shape))
            return false;

        for (Triangle tri : triangles) {
            if (tri.intersects(shape))
                return true;
        }

        return false;
    }

    @Override
    public Hit rayCast(Ray ray) {
        if (bounds.rayCast(ray) == null)
            return null;

        Hit closestHit = null;
        for (Triangle tri : triangles) {
            Hit hit = tri.rayCast(ray);
            if (hit != null && (closestHit == null || hit.tNear() < closestHit.tNear()))
                closestHit = hit;
        }

        if (closestHit != null)
            closestHit.setCollider(this);

        return closestHit;
    }

    @Override
    public Collision collide(Sphere sphere) {
        return genericCollide(sphere);
    }

    @Override
    public Collision collide(AABB aabb) {
        return genericCollide(aabb);
    }

    @Override
    public Collision collide(OBB obb) {
        return genericCollide(obb);
    }

    @Override
    public Collision collide(Plane plane) {
        return genericCollide(plane);
    }

    @Override
    public Collision collide(Triangle triangle) {
        return genericCollide(triangle);
    }

    @Override
    public Collision collide(MeshCollider mesh) {
        if (!bounds.intersects(mesh.bounds))
            return null;

        Collision bestCol = null;

        for (Triangle triA : triangles) {
            for (Triangle triB : mesh.triangles) {
                Collision col = triA.collide(triB);
                if (col != null && (bestCol == null || col.depth() > bestCol.depth()))
                    bestCol = col;
            }
        }

        if (bestCol != null)
            bestCol = new Collision(bestCol.normal(), bestCol.depth(), this, mesh);

        return bestCol;
    }

    protected Collision genericCollide(Collider<?> shape) {
        if (!bounds.intersects(shape))
            return null;

        Collision bestCol = null;

        for (Triangle tri : triangles) {
            Collision col = tri.collide(shape);
            if (col != null && (bestCol == null || col.depth() > bestCol.depth()))
                bestCol = col;
        }

        if (bestCol != null)
            bestCol = new Collision(bestCol.normal(), bestCol.depth(), this, shape);

        return bestCol;
    }

    @Override
    public Hit sweep(Sphere sphere, Vector3f velocity) {
        return genericSweep(sphere, velocity);
    }

    @Override
    public Hit sweep(AABB aabb, Vector3f velocity) {
        return genericSweep(aabb, velocity);
    }

    @Override
    public Hit sweep(OBB obb, Vector3f velocity) {
        return genericSweep(obb, velocity);
    }

    @Override
    public Hit sweep(Plane plane, Vector3f velocity) {
        //plane is infinite, we can sweep the bounds as a highly accurate proxy
        Hit hit = bounds.sweep(plane, velocity);
        if (hit != null) hit.setCollider(this);
        return hit;
    }

    @Override
    public Hit sweep(Triangle triangle, Vector3f velocity) {
        return genericSweep(triangle, velocity);
    }

    @Override
    public Hit sweep(MeshCollider mesh, Vector3f velocity) {
        if (bounds.sweep(mesh.bounds, velocity) == null && !bounds.intersects(mesh.bounds))
            return null;

        Hit bestHit = null;

        for (Triangle triA : triangles) {
            for (Triangle triB : mesh.triangles) {
                Hit hit = triA.sweep(triB, velocity);
                if (hit != null && (bestHit == null || hit.tNear() < bestHit.tNear()))
                    bestHit = hit;
            }
        }

        if (bestHit != null)
            bestHit.setCollider(this);

        return bestHit;
    }

    protected Hit genericSweep(Collider<?> shape, Vector3f velocity) {
        if (bounds.sweep(shape, velocity) == null && !bounds.intersects(shape))
            return null;

        Hit bestHit = null;

        for (Triangle tri : triangles) {
            Hit hit = tri.sweep(shape, velocity);
            if (hit != null && (bestHit == null || hit.tNear() < bestHit.tNear()))
                bestHit = hit;
        }

        if (bestHit != null)
            bestHit.setCollider(shape);

        return bestHit;
    }
}