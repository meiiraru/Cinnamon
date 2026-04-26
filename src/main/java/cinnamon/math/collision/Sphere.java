package cinnamon.math.collision;

import cinnamon.math.Maths;
import org.joml.Math;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Sphere extends Collider<Sphere> {

    private final Vector3f center = new Vector3f();
    private float radius;

    public Sphere() {
        this(0, 0, 0, 0);
    }

    public Sphere(float radius) {
        this(0, 0, 0, radius);
    }

    public Sphere(Vector3f center, float radius) {
        this(center.x, center.y, center.z, radius);
    }

    public Sphere(float x, float y, float z, float radius) {
        this.set(x, y, z, radius);
    }

    public Sphere(Sphere sphere) {
        this.set(sphere);
    }

    public Sphere(AABB aabb) {
        this.set(aabb);
    }

    public Sphere(OBB obb) {
        this.set(obb);
    }

    public Sphere set(Sphere sphere) {
        return this.set(sphere.center, sphere.radius);
    }

    public Sphere set(AABB aabb) {
        Vector3f center = aabb.getCenter();
        float radius = aabb.getDimensions().length() * 0.5f;
        return this.set(center, radius);
    }

    public Sphere set(OBB obb) {
        Vector3f center = obb.getCenter();
        float radius = obb.getHalfExtents().length();
        return this.set(center, radius);
    }

    public Sphere set(Vector3f center, float radius) {
        return this.set(center.x, center.y, center.z, radius);
    }

    public Sphere set(float x, float y, float z, float radius) {
        this.setCenter(x, y, z);
        this.setRadius(radius);
        return this;
    }

    @Override
    public Sphere clone() {
        return new Sphere(this);
    }

    @Override
    public Sphere setCenter(float x, float y, float z) {
        this.center.set(x, y, z);
        return this;
    }

    public Sphere setRadius(float radius) {
        this.radius = radius;
        return this;
    }

    @Override
    public Vector3f getCenter() {
        return center;
    }

    public float getRadius() {
        return radius;
    }

    @Override
    public Sphere translate(float x, float y, float z) {
        this.center.add(x, y, z);
        return this;
    }

    public Sphere scale(float scale) {
        this.radius *= scale;
        return this;
    }

    public Sphere applyMatrix(Matrix4f matrix) {
        Vector3f center = this.getCenter().mulPosition(matrix);
        this.setCenter(center);

        float scaleX = Vector3f.length(matrix.m00(), matrix.m10(), matrix.m20());
        float scaleY = Vector3f.length(matrix.m01(), matrix.m11(), matrix.m21());
        float scaleZ = Vector3f.length(matrix.m02(), matrix.m12(), matrix.m22());
        float maxScale = Math.max(scaleX, Math.max(scaleY, scaleZ));
        this.scale(maxScale);

        return this;
    }

    @Override
    public boolean containsPoint(float x, float y, float z) {
        return this.center.distanceSquared(x, y, z) <= radius * radius;
    }

    @Override
    public float distanceToPoint(float x, float y, float z) {
        float centerToPointDist = this.center.distance(x, y, z);
        return Math.max(0f, centerToPointDist - radius);
    }

    @Override
    public Vector3f closestPoint(float x, float y, float z, Vector3f out) {
        float dx = x - center.x;
        float dy = y - center.y;
        float dz = z - center.z;

        float distSqr = Vector3f.lengthSquared(dx, dy, dz);
        if (distSqr <= radius * radius)
            return out.set(x, y, z);

        float scale = radius / Math.sqrt(distSqr);
        return out.set(center.x + dx * scale, center.y + dy * scale, center.z + dz * scale);
    }

    @Override
    public boolean intersectsSphere(Sphere other) {
        float radiusSum = this.radius + other.radius;
        return this.center.distanceSquared(other.center) <= radiusSum * radiusSum;
    }

    @Override
    public boolean intersectsAABB(AABB aabb) {
        return aabb.intersectsSphere(this);
    }

    @Override
    public boolean intersectsOBB(OBB obb) {
        return obb.intersectsSphere(this);
    }

    @Override
    public Hit collideRay(Ray ray) {
        Vector3f dir = ray.getDirection();
        Vector3f origin = ray.getOrigin();

        //sphere center to ray origin
        float ocX = origin.x - center.x;
        float ocY = origin.y - center.y;
        float ocZ = origin.z - center.z;

        //standard quadratic formula for ray-sphere intersection
        //float a = dir.dot(dir); // always 1
        float b = dir.dot(ocX, ocY, ocZ);
        float c = (ocX * ocX + ocY * ocY + ocZ * ocZ) - radius * radius;
        float discriminant = b * b - c;
        if (discriminant < 0)
            return null;

        float sqrtD = Math.sqrt(discriminant);
        float tNear = -b - sqrtD;
        float tFar = -b + sqrtD;
        float maxDist = ray.getMaxDistance();

        //check if the sphere is entirely behind the ray or beyond max distance
        if (tFar < 0 || tNear > maxDist)
            return null;

        //normalize the intersection times
        float normNear = tNear / maxDist;
        float normFar  = tFar  / maxDist;

        //calculate raycast result
        Vector3f hitPos = origin.fma(tNear, dir, new Vector3f());

        //(hitPoint - center) / radius
        Vector3f hitNormal = new Vector3f((hitPos.x - center.x) / radius, (hitPos.y - center.y) / radius, (hitPos.z - center.z) / radius);
        return new Hit(hitPos, hitNormal, normNear, normFar, ray, this);
    }

    @Override
    public void project(Vector3f axis, float[] minMax) {
        float centerProj = center.dot(axis);
        minMax[0] = centerProj - radius;
        minMax[1] = centerProj + radius;
    }

    @Override
    public Collision collideAABB(AABB aabb) {
        Collision col = aabb.collideSphere(this);
        return col != null ? col.invert() : null;
    }

    @Override
    public Collision collideOBB(OBB obb) {
        Collision col = obb.collideSphere(this);
        return col != null ? col.invert() : null;
    }

    @Override
    public Collision collideSphere(Sphere sphere) {
        float dx = sphere.center.x - this.center.x;
        float dy = sphere.center.y - this.center.y;
        float dz = sphere.center.z - this.center.z;

        float distSq = dx * dx + dy * dy + dz * dz;
        float radiusSum = this.radius + sphere.radius;

        if (distSq > radiusSum * radiusSum)
            return null;

        float dist = Math.sqrt(distSq);
        float depth = radiusSum - dist;
        Vector3f normal = (dist > Maths.KINDA_SMALL_NUMBER) ? new Vector3f(dx / dist, dy / dist, dz / dist) : new Vector3f(1, 0, 0);

        return new Collision(normal, depth, this, sphere);
    }

    @Override
    public Hit sweepSphere(Sphere sphere, Vector3f velocity) {
        if (velocity.lengthSquared() < Maths.SMALL_NUMBER)
            return null;

        //vector from the static sphere center to the moving sphere center
        float ocX = this.center.x - sphere.center.x;
        float ocY = this.center.y - sphere.center.y;
        float ocZ = this.center.z - sphere.center.z;

        float radiusSum = this.radius + sphere.radius;

        //check if they are already overlapping at the start
        float ocLengthSq = ocX * ocX + ocY * ocY + ocZ * ocZ;
        if (ocLengthSq <= radiusSum * radiusSum) {
            return null;
        }

        //check if the moving sphere is moving away from the static one
        float dot = ocX * velocity.x + ocY * velocity.y + ocZ * velocity.z;
        if (dot >= 0)
            return null;

        //set up the quadratic equation At squared + Bt + C = 0
        //A = v dot v
        float a = velocity.dot(velocity);
        //B = 2 * (oc dot v)
        float b = 2f * dot;
        //C = oc dot oc - (r1 + r2) squared
        float c = ocLengthSq - radiusSum * radiusSum;

        //calculate the discriminant (B squared - 4AC)
        float discriminant = b * b - 4f * a * c;

        //if the discriminant is negative, the ray misses the sphere
        if (discriminant < 0f)
            return null;

        //solve for the smallest positive root, which is the time of first contact
        float sqrtD = Math.sqrt(discriminant);
        float tNear = (-b - sqrtD) / (2f * a);

        //check if the collision time is outside the frame interval [0, 1]
        if (tNear < 0f || tNear > 1f)
            return null;

        //build the return hit
        float tFar = (-b + sqrtD) / (2f * a);
        Ray ray = new Ray(this.center, velocity, velocity.length());

        //calculate normal
        Vector3f centerAtImpact = this.center.fma(tNear, velocity, new Vector3f());
        Vector3f normal = centerAtImpact.sub(sphere.center, centerAtImpact).normalize();

        //calculate hit position
        Vector3f hitPosition = sphere.center.fma(sphere.radius, normal, new Vector3f());
        return new Hit(hitPosition, normal, tNear, tFar, ray, sphere);
    }

    @Override
    public Hit sweepAABB(AABB aabb, Vector3f velocity) {
        if (velocity.lengthSquared() < Maths.SMALL_NUMBER)
            return null;

        //try minkowski sum first to quickly rule out non-collisions
        float expMinX = aabb.minX() - radius, expMinY = aabb.minY() - radius, expMinZ = aabb.minZ() - radius;
        float expMaxX = aabb.maxX() + radius, expMaxY = aabb.maxY() + radius, expMaxZ = aabb.maxZ() + radius;

        //perform a slab test with the sphere center as the ray origin
        float invX = 1f / velocity.x, invY = 1f / velocity.y, invZ = 1f / velocity.z;
        float t1 = (expMinX - center.x) * invX, t2 = (expMaxX - center.x) * invX;
        float t3 = (expMinY - center.y) * invY, t4 = (expMaxY - center.y) * invY;
        float t5 = (expMinZ - center.z) * invZ, t6 = (expMaxZ - center.z) * invZ;

        float tMin = Math.max(Math.max(Math.min(t1, t2), Math.min(t3, t4)), Math.min(t5, t6));
        float tMax = Math.min(Math.min(Math.max(t1, t2), Math.max(t3, t4)), Math.max(t5, t6));

        //if there is no intersection in the interval [0, 1]
        if (!(tMin <= tMax && tMin <= 1f && tMax >= 0f)) //negated inclusive check to avoid NaN
            return null;

        //find the exact point of collision
        Vector3f temp = new Vector3f();
        Vector3f contactPoint = center.fma(tMin, velocity, temp);

        //find the closest point on the aabb to this contact point
        Vector3f closestPointOnAABB = aabb.closestPoint(contactPoint, temp);

        //solve the precise intersection time between the moving sphere and the static point
        Vector3f oc = center.sub(closestPointOnAABB, temp);

        float a = velocity.dot(velocity);
        float b = 2f * velocity.dot(oc);
        float c = oc.dot(oc) - radius * radius;

        float discriminant = b * b - 4f * a * c;
        //should not happen if broad phase passed, but good for safety
        if (discriminant < 0f)
            return null;

        float tHit = (-b - Math.sqrt(discriminant)) / (2f * a);
        //if the precise hit time is outside the range, no collision
        if (!(tHit >= 0f && tHit <= 1f)) //negated inclusive check to avoid NaN
            return null;

        //build the hit result
        Ray ray = new Ray(center, velocity, velocity.length());

        //calculate position
        Vector3f centerAtImpact = center.fma(tHit, velocity, temp);
        Vector3f hitPosition = aabb.closestPoint(centerAtImpact, new Vector3f());

        //calculate normal
        Vector3f normal = centerAtImpact.sub(hitPosition, temp).normalize();
        return new Hit(hitPosition, normal, tHit, tMax, ray, aabb);
    }

    @Override
    public Hit sweepOBB(OBB obb, Vector3f velocity) {
        if (velocity.lengthSquared() < Maths.SMALL_NUMBER)
            return null;

        //transform into OBB local space
        Vector3f obbCenter = obb.getCenter();
        Vector3f obbHalfExtents = obb.getHalfExtents();
        Vector3f obbAxisX = obb.getAxisX(), obbAxisY = obb.getAxisY(), obbAxisZ = obb.getAxisZ();

        //transform sphere center relative to OBB center
        float rx = center.x - obbCenter.x;
        float ry = center.y - obbCenter.y;
        float rz = center.z - obbCenter.z;

        //project sphere center and velocity onto OBB axes
        float localX = rx * obbAxisX.x + ry * obbAxisX.y + rz * obbAxisX.z;
        float localY = rx * obbAxisY.x + ry * obbAxisY.y + rz * obbAxisY.z;
        float localZ = rx * obbAxisZ.x + ry * obbAxisZ.y + rz * obbAxisZ.z;

        float localVelX = velocity.dot(obbAxisX);
        float localVelY = velocity.dot(obbAxisY);
        float localVelZ = velocity.dot(obbAxisZ);

        //now we have an AABB
        float expMinX = -obbHalfExtents.x - radius, expMinY = -obbHalfExtents.y - radius, expMinZ = -obbHalfExtents.z - radius;
        float expMaxX =  obbHalfExtents.x + radius, expMaxY =  obbHalfExtents.y + radius, expMaxZ =  obbHalfExtents.z + radius;

        //perform a slab test with the sphere center as the ray origin
        float invX = 1f / localVelX, invY = 1f / localVelY, invZ = 1f / localVelZ;
        float t1 = (expMinX - localX) * invX, t2 = (expMaxX - localX) * invX;
        float t3 = (expMinY - localY) * invY, t4 = (expMaxY - localY) * invY;
        float t5 = (expMinZ - localZ) * invZ, t6 = (expMaxZ - localZ) * invZ;

        float tMin = Math.max(Math.max(Math.min(t1, t2), Math.min(t3, t4)), Math.min(t5, t6));
        float tMax = Math.min(Math.min(Math.max(t1, t2), Math.max(t3, t4)), Math.max(t5, t6));

        if (!(tMin <= tMax && tMin <= 1f && tMax >= 0f))
            return null;

        //find the exact point of collision
        Vector3f temp = new Vector3f();
        Vector3f localContactPoint = temp.set(localX + tMin * localVelX, localY + tMin * localVelY, localZ + tMin * localVelZ);
        Vector3f localClosestPoint = temp.set(
                Maths.clamp(localContactPoint.x, -obbHalfExtents.x, obbHalfExtents.x),
                Maths.clamp(localContactPoint.y, -obbHalfExtents.y, obbHalfExtents.y),
                Maths.clamp(localContactPoint.z, -obbHalfExtents.z, obbHalfExtents.z)
        );

        //solve the precise intersection time between the moving sphere and the static point
        Vector3f localOc = temp.set(localX - localClosestPoint.x, localY - localClosestPoint.y, localZ - localClosestPoint.z);

        float a = Vector3f.lengthSquared(localVelX, localVelY, localVelZ);
        float b = 2f * (localVelX * localOc.x + localVelY * localOc.y + localVelZ * localOc.z);
        float c = localOc.dot(localOc) - radius * radius;

        float discriminant = b * b - 4f * a * c;
        if (discriminant < 0f)
            return null;

        float tHit = (-b - Math.sqrt(discriminant)) / (2f * a);
        if (!(tHit >= 0f && tHit <= 1f))
            return null;

        //transform back to world space and build the hit result
        Ray ray = new Ray(center, velocity, velocity.length());

        //calculate position
        Vector3f centerAtImpact = center.fma(tHit, velocity, temp);
        Vector3f hitPosition = obb.closestPoint(centerAtImpact, new Vector3f());

        //calculate normal
        Vector3f normal = centerAtImpact.sub(hitPosition, temp).normalize();

        //if the sphere starts inside the OBB, the normal might be zero
        if (normal.lengthSquared() < Maths.KINDA_SMALL_NUMBER)
            normal.set(velocity).negate().normalize();

        return new Hit(hitPosition, normal, tHit, tMax, ray, obb);
    }

    @Override
    public String toString() {
        return "Sphere{x=" + center.x + " y=" + center.y + " z=" + center.z + " r=" + radius + "}";
    }
}
