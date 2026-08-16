package cinnamon.math.collision;

import cinnamon.math.Maths;
import org.joml.Math;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Plane extends Collider<Plane> {

    private final Vector3f normal = new Vector3f(0, 1, 0);
    private float distance = 0f;

    public Plane() {
        this(0, 1, 0, 0);
    }

    public Plane(Vector3f normal, float distance) {
        this(normal.x, normal.y, normal.z, distance);
    }

    public Plane(float nx, float ny, float nz, float distance) {
        this.set(nx, ny, nz, distance);
    }

    public Plane(Plane plane) {
        this.set(plane);
    }

    public Plane set(Plane plane) {
        return this.set(plane.normal.x, plane.normal.y, plane.normal.z, plane.distance);
    }

    public Plane set(float nx, float ny, float nz, float distance) {
        this.normal.set(nx, ny, nz).normalize();
        this.distance = distance;
        return this;
    }

    public Vector3f getNormal() {
        return normal;
    }

    public float getDistance() {
        return distance;
    }

    @Override
    public Plane clone() {
        return new Plane(this);
    }

    @Override
    public AABB toAABB() {
        return new AABB().set(this);
    }

    @Override
    public Vector3f getCenter() {
        return new Vector3f(normal.x * distance, normal.y * distance, normal.z * distance);
    }

    @Override
    public Plane setCenter(float x, float y, float z) {
        this.distance = normal.x * x + normal.y * y + normal.z * z;
        return this;
    }

    @Override
    public float getVolume() {
        return 0f;
    }

    @Override
    public Vector3f getRandomPoint() {
        Vector3f tangent = new Vector3f();
        if (Math.abs(normal.x) > 0.5f) {
            tangent.set(normal.y, -normal.x, 0f).normalize();
        } else {
            tangent.set(0f, normal.z, -normal.y).normalize();
        }
        Vector3f bitangent = new Vector3f(normal).cross(tangent);

        float u = Maths.range(-100f, 100f);
        float v = Maths.range(-100f, 100f);
        return getCenter().add(tangent.mul(u)).add(bitangent.mul(v));
    }

    @Override
    public Plane translate(float x, float y, float z) {
        this.distance += normal.x * x + normal.y * y + normal.z * z;
        return this;
    }

    @Override
    public Plane applyMatrix(Matrix4f matrix) {
        if ((matrix.properties() & Matrix4f.PROPERTY_IDENTITY) != 0)
            return this;

        //rotate the plane by the matrix rotation
        matrix.transformDirection(this.normal).normalize();

        //translate the plane by the matrix translation
        return this.setCenter(matrix.getTranslation(getCenter()));
    }

    @Override
    public boolean containsPoint(float x, float y, float z) {
        return this.distanceToPoint(x, y, z) <= Maths.KINDA_SMALL_NUMBER;
    }

    @Override
    public float distanceToPoint(float x, float y, float z) {
        return Math.abs(normal.x * x + normal.y * y + normal.z * z - distance);
    }

    @Override
    public Vector3f closestPoint(float x, float y, float z, Vector3f out) {
        float dist = normal.x * x + normal.y * y + normal.z * z - distance;
        return out.set(x - normal.x * dist, y - normal.y * dist, z - normal.z * dist);
    }

    @Override
    public boolean intersects(Sphere sphere) {
        return normal.dot(sphere.getCenter()) - distance <= sphere.getRadius();
    }

    @Override
    public boolean intersects(AABB aabb) {
        float d = normal.dot(aabb.getCenter()) - distance;
        float r = aabb.getWidth()  * 0.5f * Math.abs(normal.x) +
                  aabb.getHeight() * 0.5f * Math.abs(normal.y) +
                  aabb.getDepth()  * 0.5f * Math.abs(normal.z);
        return d <= r;
    }

    @Override
    public boolean intersects(OBB obb) {
        float d = normal.dot(obb.getCenter()) - distance;
        float r = obb.getHalfExtents().x * Math.abs(normal.dot(obb.getAxisX())) +
                  obb.getHalfExtents().y * Math.abs(normal.dot(obb.getAxisY())) +
                  obb.getHalfExtents().z * Math.abs(normal.dot(obb.getAxisZ()));
        return d <= r;
    }

    @Override
    public boolean intersects(Plane plane) {
        float dot = normal.dot(plane.getNormal());

        //if the planes face opposite directions
        if (dot < -0.999f) {
            //only overlap if the distances are equal or greater than zero
            return this.distance + plane.distance >= 0;
        }

        //same direction or angled planes will always intersect at a 3D line
        return true;
    }

    @Override
    public boolean intersects(MeshCollider mesh) {
        return mesh.intersects(this);
    }

    @Override
    public Hit rayCast(Ray ray) {
        float nd = normal.dot(ray.getDirection());
        if (nd >= -Maths.KINDA_SMALL_NUMBER)
            return null; //ray is parallel to the plane

        float t = (distance - normal.dot(ray.getOrigin())) / nd;
        float maxDist = ray.getMaxDistance();

        if (t < 0 || t > maxDist)
            return null; //behind origin or further than max distance

        float normT = t / maxDist;
        Vector3f hitPos = ray.getOrigin().fma(t, ray.getDirection(), new Vector3f());
        return new Hit(hitPos, new Vector3f(normal), normT, normT, ray, this);
    }

    @Override
    public void project(Vector3f axis, float[] minMax) {
        float dot = Math.abs(normal.dot(axis));
        if (dot > 0.999f) {
            float proj = distance * normal.dot(axis);
            minMax[0] = proj;
            minMax[1] = proj;
        } else {
            minMax[0] = -Float.MAX_VALUE;
            minMax[1] = Float.MAX_VALUE;
        }
    }

    @Override
    public Collision collide(Sphere sphere) {
        float d = normal.dot(sphere.getCenter()) - distance;
        if (d > sphere.getRadius())
            return null;

        float depth = sphere.getRadius() - d;
        return new Collision(new Vector3f(normal), depth, this, sphere);
    }

    @Override
    public Collision collide(AABB aabb) {
        float d = normal.dot(aabb.getCenter()) - distance;
        float r = aabb.getWidth()  * 0.5f * Math.abs(normal.x) +
                  aabb.getHeight() * 0.5f * Math.abs(normal.y) +
                  aabb.getDepth()  * 0.5f * Math.abs(normal.z);

        if (d > r)
            return null;

        float depth = r - d;
        return new Collision(new Vector3f(normal), depth, this, aabb);
    }

    @Override
    public Collision collide(OBB obb) {
        float d = normal.dot(obb.getCenter()) - distance;
        float r = obb.getHalfExtents().x * Math.abs(normal.dot(obb.getAxisX())) +
                  obb.getHalfExtents().y * Math.abs(normal.dot(obb.getAxisY())) +
                  obb.getHalfExtents().z * Math.abs(normal.dot(obb.getAxisZ()));

        if (d > r)
            return null;

        float depth = r - d;
        return new Collision(new Vector3f(normal), depth, this, obb);
    }

    @Override
    public Collision collide(Plane plane) {
        float dot = normal.dot(plane.getNormal());

        if (dot < -0.999f) {
            float depth = this.distance + plane.distance;
            if (depth >= 0)
                return new Collision(new Vector3f(normal).negate(), depth, this, plane);
            return null; //empty gap between them
        }

        //infinite overlap for angled/same-direction planes
        return new Collision(new Vector3f(normal).negate(), Float.MAX_VALUE, this, plane);
    }

    @Override
    public Collision collide(MeshCollider mesh) {
        return invertCollide(mesh.collide(this));
    }

    private Hit sweepStaticPlane(Collider<?> shape, float r, Vector3f velocity) {
        float vDir = normal.dot(velocity);
        float d0 = normal.dot(shape.getCenter()) - distance;

        if (d0 <= r)
            return null; //already intersecting or behind
        if (vDir >= -Maths.KINDA_SMALL_NUMBER)
            return null; //moving parallel or away from the plane

        float t = (r - d0) / vDir;
        if (t > 1f || t < 0f)
            return null;

        Vector3f shapeCenterAtHit = shape.getCenter().fma(t, velocity, new Vector3f());
        Vector3f hitPos = shapeCenterAtHit.sub(normal.x * r, normal.y * r, normal.z * r, new Vector3f());
        Ray ray = new Ray(shape.getCenter(), velocity, velocity.length());
        return new Hit(hitPos, new Vector3f(normal), t, t, ray, this);
    }

    @Override
    public Hit sweep(Sphere sphere, Vector3f velocity) {
        Hit hit = sweepStaticPlane(sphere, sphere.getRadius(), new Vector3f(velocity).negate());
        return invertSweep(hit, sphere, velocity);
    }

    @Override
    public Hit sweep(AABB aabb, Vector3f velocity) {
        float r = aabb.getWidth()  * 0.5f * Math.abs(normal.x) +
                  aabb.getHeight() * 0.5f * Math.abs(normal.y) +
                  aabb.getDepth()  * 0.5f * Math.abs(normal.z);
        Hit hit = sweepStaticPlane(aabb, r, new Vector3f(velocity).negate());
        return invertSweep(hit, aabb, velocity);
    }

    @Override
    public Hit sweep(OBB obb, Vector3f velocity) {
        float r = obb.getHalfExtents().x * Math.abs(normal.dot(obb.getAxisX())) +
                  obb.getHalfExtents().y * Math.abs(normal.dot(obb.getAxisY())) +
                  obb.getHalfExtents().z * Math.abs(normal.dot(obb.getAxisZ()));
        Hit hit = sweepStaticPlane(obb, r, new Vector3f(velocity).negate());
        return invertSweep(hit, obb, velocity);
    }

    @Override
    public Hit sweep(Plane plane, Vector3f velocity) {
        //if they already intersect, a sweep does not trigger
        if (intersects(plane))
            return null;

        //if they do not intersect, they must be opposite-facing with a gap
        float vn = normal.dot(velocity);

        //to close the gap, the moving plane must be traveling in the direction of its normal
        if (vn <= Maths.KINDA_SMALL_NUMBER)
            return null;

        //the distance needed to travel to close the gap
        float t = -(this.distance + plane.distance) / vn;
        if (t < 0f || t > 1f)
            return null;

        //the hit normal is the normal of the static plane we just hit
        Vector3f hitNormal = new Vector3f(plane.getNormal());
        Vector3f hitPos = this.getCenter().fma(t, velocity, new Vector3f());
        Ray ray = new Ray(this.getCenter(), velocity, velocity.length());
        return new Hit(hitPos, hitNormal, t, t, ray, plane);
    }

    @Override
    public Hit sweep(MeshCollider mesh, Vector3f velocity) {
        Hit hit = mesh.sweep(this, new Vector3f(velocity).negate());
        return invertSweep(hit, mesh, velocity);
    }

    @Override
    public String toString() {
        return "Plane{nx=" + normal.x + " ny=" + normal.y + " nz=" + normal.z + " d=" + distance + "}";
    }
}
