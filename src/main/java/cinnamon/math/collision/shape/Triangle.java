package cinnamon.math.collision.shape;

import cinnamon.math.Maths;
import cinnamon.math.collision.Collider;
import cinnamon.math.collision.Collision;
import cinnamon.math.collision.Hit;
import cinnamon.math.collision.Ray;
import cinnamon.math.collision.SATHelper;
import org.joml.Math;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Triangle extends Collider<Triangle> {

    public final Vector3f v0 = new Vector3f();
    public final Vector3f v1 = new Vector3f();
    public final Vector3f v2 = new Vector3f();
    public final Vector3f center = new Vector3f();
    public final Vector3f[] axes = new Vector3f[]{new Vector3f(), new Vector3f(), new Vector3f(), new Vector3f()};

    public Triangle() {}

    public Triangle(Vector3f a, Vector3f b, Vector3f c) {
        set(a, b, c);
    }

    public Triangle set(Vector3f a, Vector3f b, Vector3f c) {
        Vector3f normal = new Vector3f(b.x - a.x, b.y - a.y, b.z - a.z).cross(c.x - a.x, c.y - a.y, c.z - a.z).normalize();
        return set(a, b, c, normal);
    }

    public Triangle set(Vector3f a, Vector3f b, Vector3f c, Vector3f normal) {
        v0.set(a);
        v1.set(b);
        v2.set(c);
        center.set((a.x + b.x + c.x) / 3f, (a.y + b.y + c.y) / 3f, (a.z + b.z + c.z) / 3f);

        axes[0].set(normal);
        axes[1].set(v1.x - v0.x, v1.y - v0.y, v1.z - v0.z).normalize();
        axes[2].set(v2.x - v1.x, v2.y - v1.y, v2.z - v1.z).normalize();
        axes[3].set(v0.x - v2.x, v0.y - v2.y, v0.z - v2.z).normalize();
        return this;
    }

    public Vector3f getNormal() {
        return axes[0];
    }

    @Override
    public Triangle clone() {
        return new Triangle().set(v0, v1, v2, axes[0]);
    }

    @Override
    public AABB toAABB() {
        AABB aabb = new AABB();
        aabb.set(v0, v0);
        aabb.include(v1);
        aabb.include(v2);
        return aabb;
    }

    @Override
    public Vector3f getCenter() {
        return center;
    }

    @Override
    public Triangle setCenter(float x, float y, float z) {
        Vector3f diff = new Vector3f(x - center.x, y - center.y, z - center.z);
        return translate(diff.x, diff.y, diff.z);
    }

    @Override
    public float getVolume() {
        return 0f;
    }

    @Override
    public Vector3f getRandomPoint(Vector3f out) {
        float u = (float) Math.random();
        float v = (float) Math.random();
        if (u + v > 1f) {
            u = 1f - u;
            v = 1f - v;
        }
        float w = 1f - u - v;
        return out.set(
                v0.x * u + v1.x * v + v2.x * w,
                v0.y * u + v1.y * v + v2.y * w,
                v0.z * u + v1.z * v + v2.z * w
        );
    }

    @Override
    public Triangle translate(float x, float y, float z) {
        v0.add(x, y, z);
        v1.add(x, y, z);
        v2.add(x, y, z);
        center.add(x, y, z);
        return this;
    }

    @Override
    public Triangle applyMatrix(Matrix4f matrix) {
        if ((matrix.properties() & Matrix4f.PROPERTY_IDENTITY) != 0)
            return this;

        v0.mulPosition(matrix);
        v1.mulPosition(matrix);
        v2.mulPosition(matrix);

        //recalculate normal and center
        return set(v0, v1, v2);
    }

    @Override
    public void project(Vector3f axis, float[] minMax) {
        float p0 = v0.dot(axis), p1 = v1.dot(axis), p2 = v2.dot(axis);
        minMax[0] = Math.min(p0, Math.min(p1, p2));
        minMax[1] = Math.max(p0, Math.max(p1, p2));
    }

    @Override
    public boolean containsPoint(float x, float y, float z) {
        Vector3f closest = new Vector3f();
        closestPoint(x, y, z, closest);
        return closest.distanceSquared(x, y, z) <= Maths.KINDA_SMALL_NUMBER;
    }

    @Override
    public float distanceToPoint(float x, float y, float z) {
        Vector3f closest = new Vector3f();
        closestPoint(x, y, z, closest);
        return closest.distance(x, y, z);
    }

    @Override
    public Vector3f closestPoint(float x, float y, float z, Vector3f out) {
        //ab, ac, ap = b, c, p - a
        float abx = v1.x - v0.x, aby = v1.y - v0.y, abz = v1.z - v0.z;
        float acx = v2.x - v0.x, acy = v2.y - v0.y, acz = v2.z - v0.z;
        float apx =    x - v0.x, apy =    y - v0.y, apz =    z - v0.z;

        //d1, d2 = ab, ac dot ap
        float d1 = abx * apx + aby * apy + abz * apz;
        float d2 = acx * apx + acy * apy + acz * apz;
        if (d1 <= 0f && d2 <= 0f)
            return out.set(v0);

        //bp = p - b
        float bpx = x - v1.x, bpy = y - v1.y, bpz = z - v1.z;

        //d3, d4 = ab, ac dot bp
        float d3 = abx * bpx + aby * bpy + abz * bpz;
        float d4 = acx * bpx + acy * bpy + acz * bpz;
        if (d3 >= 0f && d4 <= d3)
            return out.set(v1);

        float vc = d1 * d4 - d3 * d2;
        if (vc <= 0f && d1 >= 0f && d3 <= 0f) {
            float v = d1 / (d1 - d3);
            return out.set(v0.x + abx * v, v0.y + aby * v, v0.z + abz * v);
        }

        //cp = p - c
        float cpx = x - v2.x, cpy = y - v2.y, cpz = z - v2.z;

        float d5 = abx * cpx + aby * cpy + abz * cpz;
        float d6 = acx * cpx + acy * cpy + acz * cpz;
        if (d6 >= 0f && d5 <= d6)
            return out.set(v2);

        float vb = d5 * d2 - d1 * d6;
        if (vb <= 0f && d2 >= 0f && d6 <= 0f) {
            float w = d2 / (d2 - d6);
            return out.set(v0.x + acx * w, v0.y + acy * w, v0.z + acz * w);
        }

        float va = d3 * d6 - d5 * d4;
        if (va <= 0f && (d4 - d3) >= 0f && (d5 - d6) >= 0f) {
            float w = (d4 - d3) / ((d4 - d3) + (d5 - d6));
            return out.set(v1.x + (v2.x - v1.x) * w, v1.y + (v2.y - v1.y) * w, v1.z + (v2.z - v1.z) * w);
        }

        float denom = 1f / (va + vb + vc);
        float v = vb * denom;
        float w = vc * denom;
        return out.set(v0.x + abx * v + acx * w, v0.y + aby * v + acy * w, v0.z + abz * v + acz * w);
    }

    @Override
    public Hit rayCast(Ray ray) {
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
        Vector3f hitNorm = new Vector3f(axes[0]);
        if (a < 0f) hitNorm.negate();

        float normT = t / ray.getMaxDistance();
        return new Hit(hitPos, hitNorm, normT, normT, ray, this);
    }

    @Override
    public boolean intersects(Sphere sphere) {
        return collide(sphere) != null;
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
        return collide(plane) != null;
    }

    @Override
    public boolean intersects(Triangle triangle) {
        if (Math.max(v0.x, Math.max(v1.x, v2.x)) < Math.min(triangle.v0.x, Math.min(triangle.v1.x, triangle.v2.x))) return false;
        if (Math.min(v0.x, Math.min(v1.x, v2.x)) > Math.max(triangle.v0.x, Math.max(triangle.v1.x, triangle.v2.x))) return false;
        if (Math.max(v0.y, Math.max(v1.y, v2.y)) < Math.min(triangle.v0.y, Math.min(triangle.v1.y, triangle.v2.y))) return false;
        if (Math.min(v0.y, Math.min(v1.y, v2.y)) > Math.max(triangle.v0.y, Math.max(triangle.v1.y, triangle.v2.y))) return false;
        if (Math.max(v0.z, Math.max(v1.z, v2.z)) < Math.min(triangle.v0.z, Math.min(triangle.v1.z, triangle.v2.z))) return false;
        if (Math.min(v0.z, Math.min(v1.z, v2.z)) > Math.max(triangle.v0.z, Math.max(triangle.v1.z, triangle.v2.z))) return false;
        return true;
    }

    @Override
    public boolean intersects(MeshCollider mesh) {
        return mesh.intersects(this);
    }

    @Override
    public Collision collide(Sphere sphere) {
        Vector3f spCenter = sphere.getCenter();
        Vector3f cp = this.closestPoint(spCenter.x, spCenter.y, spCenter.z, new Vector3f());

        float distSq = spCenter.distanceSquared(cp);
        float rSq = sphere.getRadius() * sphere.getRadius();

        if (distSq > rSq)
            return null;

        float dist = Math.sqrt(distSq);
        float depth = sphere.getRadius() - dist;
        Vector3f colNormal = new Vector3f();
        if (dist > Maths.KINDA_SMALL_NUMBER) {
            colNormal.set((spCenter.x - cp.x) / dist, (spCenter.y - cp.y) / dist, (spCenter.z - cp.z) / dist);
        } else {
            colNormal.set(axes[0]);
        }
        return new Collision(colNormal, depth, this, sphere);
    }

    @Override
    public Collision collide(AABB aabb) {
        return SATHelper.SATCollide(this, aabb, axes, SATHelper.AABB_AXES);
    }

    @Override
    public Collision collide(OBB obb) {
        return SATHelper.SATCollide(this, obb, axes, obb.getAxes());
    }

    @Override
    public Collision collide(Plane plane) {
        float maxDepth = -Float.MAX_VALUE;
        Vector3f colNorm = new Vector3f(plane.getNormal()).negate();

        for (Vector3f v : new Vector3f[]{v0, v1, v2}) {
            float d = plane.getNormal().dot(v) - plane.getDistance();
            if (d <= 0) {
                float depth = -d;
                if (depth > maxDepth)
                    maxDepth = depth;
            }
        }
        return maxDepth >= 0f ? new Collision(colNorm, maxDepth, this, plane) : null;
    }

    @Override
    public Collision collide(Triangle triangle) {
        return intersects(triangle) ? SATHelper.SATCollide(this, triangle, axes, triangle.axes) : null;
    }

    @Override
    public Collision collide(MeshCollider mesh) {
        return invertCollide(mesh.collide(this));
    }

    @Override
    public Hit sweep(Sphere sphere, Vector3f velocity) {
        if (velocity.lengthSquared() <= Maths.KINDA_SMALL_NUMBER)
            return null;

        Vector3f N = axes[0];
        float radius = sphere.getRadius();
        Vector3f C = sphere.getCenter();

        float minT = Float.MAX_VALUE;
        Vector3f hitNormal = null;
        Vector3f hitPos = null;

        //face sweep (ray against the triangle plane offset by radius)
        float vDotN = velocity.dot(N);
        if (Math.abs(vDotN) > Maths.KINDA_SMALL_NUMBER) {
            float distToPlane = (C.x - v0.x) * N.x + (C.y - v0.y) * N.y + (C.z - v0.z) * N.z;
            float targetDist = (vDotN < 0) ? radius : -radius; //sweep towards front or back face
            float t = (targetDist - distToPlane) / vDotN;

            if (t >= 0f && t <= 1f) {
                Vector3f centerAtT = new Vector3f(C).fma(t, velocity);
                Vector3f pointOnPlane = new Vector3f(centerAtT).sub(new Vector3f(N).mul(targetDist));

                //check if the contact point is inside the triangle edges
                Vector3f cp = this.closestPoint(pointOnPlane.x, pointOnPlane.y, pointOnPlane.z, new Vector3f());
                if (cp.distanceSquared(pointOnPlane) <= Maths.KINDA_SMALL_NUMBER) {
                    minT = t;
                    hitPos = pointOnPlane;
                    hitNormal = new Vector3f(N).mul(vDotN < 0 ? 1 : -1);
                }
            }
        }

        //vertex sweeps (ray vs sphere intersection at each vertex)
        Vector3f[] verts = {v0, v1, v2};
        float velSq = velocity.lengthSquared();
        for (Vector3f v : verts) {
            Vector3f m = new Vector3f(C).sub(v);
            float b = m.dot(velocity);
            float c = m.lengthSquared() - radius * radius;
            float disc = b * b - velSq * c;

            if (disc >= 0f) {
                float t = -(b + Math.sqrt(disc)) / velSq;
                if (t >= 0f && t <= 1f && t < minT) {
                    minT = t;
                    Vector3f centerAtT = new Vector3f(C).fma(t, velocity);
                    hitPos = new Vector3f(v);
                    hitNormal = new Vector3f(centerAtT).sub(v).normalize();
                }
            }
        }

        //edge sweeps (ray vs cylinder intersection along each edge)
        for (int i = 0; i < 3; i++) {
            Vector3f p1 = verts[i];
            Vector3f p2 = verts[(i + 1) % 3];
            Vector3f edge = new Vector3f(p2).sub(p1);
            float edgeLen2 = edge.lengthSquared();

            Vector3f dp = new Vector3f(C).sub(p1);
            float velDotEdge = velocity.dot(edge);
            float dpDotEdge = dp.dot(edge);

            float A = edgeLen2 * velSq - velDotEdge * velDotEdge;
            float B = edgeLen2 * velocity.dot(dp) - velDotEdge * dpDotEdge;
            float C_q = edgeLen2 * dp.lengthSquared() - dpDotEdge * dpDotEdge - radius * radius * edgeLen2;

            if (Math.abs(A) > Maths.KINDA_SMALL_NUMBER) {
                float disc = B * B - A * C_q;
                if (disc >= 0f) {
                    float t = -(B + Math.sqrt(disc)) / A;
                    if (t >= 0f && t <= 1f && t < minT) {
                        //project the hit point onto the edge to ensure it did not hit the infinite line past the vertices
                        float proj = (dpDotEdge + t * velDotEdge) / edgeLen2;
                        if (proj >= 0f && proj <= 1f) {
                            minT = t;
                            Vector3f centerAtT = new Vector3f(C).fma(t, velocity);
                            hitPos = new Vector3f(p1).fma(proj, edge);
                            hitNormal = new Vector3f(centerAtT).sub(hitPos).normalize();
                        }
                    }
                }
            }
        }

        if (minT <= 1f) {
            Ray ray = new Ray(sphere.getCenter(), velocity, velocity.length());
            return new Hit(hitPos, hitNormal, minT, minT, ray, sphere);
        }

        return null;
    }

    @Override
    public Hit sweep(AABB aabb, Vector3f velocity) {
        return SATHelper.SATSweep(this, aabb, axes, SATHelper.AABB_AXES, velocity);
    }

    @Override
    public Hit sweep(OBB obb, Vector3f velocity) {
        return SATHelper.SATSweep(this, obb, axes, obb.getAxes(), velocity);
    }

    @Override
    public Hit sweep(Plane plane, Vector3f velocity) {
        return invertSweep(plane.sweep(this, new Vector3f(velocity).negate()), this, velocity);
    }

    @Override
    public Hit sweep(Triangle triangle, Vector3f velocity) {
        float minAX = Math.min(v0.x, Math.min(v1.x, v2.x));
        float maxAX = Math.max(v0.x, Math.max(v1.x, v2.x));
        if (velocity.x < 0f) minAX += velocity.x; else maxAX += velocity.x;
        float minBX = Math.min(triangle.v0.x, Math.min(triangle.v1.x, triangle.v2.x));
        float maxBX = Math.max(triangle.v0.x, Math.max(triangle.v1.x, triangle.v2.x));
        if (maxAX < minBX || minAX > maxBX)
            return null;

        float minAY = Math.min(v0.y, Math.min(v1.y, v2.y));
        float maxAY = Math.max(v0.y, Math.max(v1.y, v2.y));
        if (velocity.y < 0) minAY += velocity.y; else maxAY += velocity.y;
        float minBY = Math.min(triangle.v0.y, Math.min(triangle.v1.y, triangle.v2.y));
        float maxBY = Math.max(triangle.v0.y, Math.max(triangle.v1.y, triangle.v2.y));
        if (maxAY < minBY || minAY > maxBY)
            return null;

        float minAZ = Math.min(v0.z, Math.min(v1.z, v2.z));
        float maxAZ = Math.max(v0.z, Math.max(v1.z, v2.z));
        if (velocity.z < 0) minAZ += velocity.z; else maxAZ += velocity.z;
        float minBZ = Math.min(triangle.v0.z, Math.min(triangle.v1.z, triangle.v2.z));
        float maxBZ = Math.max(triangle.v0.z, Math.max(triangle.v1.z, triangle.v2.z));
        if (maxAZ < minBZ || minAZ > maxBZ)
            return null;

        return SATHelper.SATSweep(this, triangle, axes, triangle.axes, velocity);
    }

    @Override
    public Hit sweep(MeshCollider mesh, Vector3f velocity) {
        return invertSweep(mesh.sweep(this, new Vector3f(velocity).negate()), this, velocity);
    }
}